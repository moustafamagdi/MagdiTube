// Dedicated emulator only: exercises the real phone Activity, not a DOM imitation.
// Dismiss notification/diagnostics prompts first; opt out of diagnostics on test devices.
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {mkdirSync,writeFileSync} from 'node:fs';
const args=['-P',process.env.ADB_PORT||'5038','-s',process.env.ADB_DEVICE||'emulator-5560'];
const adb=(...a)=>execFileSync(process.env.ADB||'adb',[...args,...a],{maxBuffer:16*1024*1024});
const shell=(...a)=>adb('shell',...a).toString().trim();
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const font=shell('settings','get','system','font_scale');
const size=shell('wm','size').match(/Override size: (\S+)/)?.[1]||'reset';
const density=shell('wm','density').match(/Override density: (\S+)/)?.[1]||'reset';
const locale=shell('cmd','locale','get-app-locales','com.magdi.tube').match(/\[(.*)\]/)?.[1]||'';
const labels={en:['Play','Pause','Stop','−10s','+10s','Previous','Next','Retry','Car fullscreen','Fill screen','Open on car'],ar:['تشغيل','إيقاف مؤقت','إيقاف','−10 ث','+10 ث','السابق','التالي','إعادة المحاولة','ملء شاشة العربية','بدون حواف','فتح على العربية']};
const output=process.env.SCREENSHOT_DIR||'build/controller-layout-proof';mkdirSync(output,{recursive:true});
try{
 for(const language of ['en','ar'])for(const [width,scale] of [[360,1],[320,1.3],[832,1]]){
  shell('am','force-stop','com.magdi.tube');shell('wm','size',`${width*2}x${width>700?960:1560}`);shell('wm','density','320');
  shell('settings','put','system','font_scale',String(scale));shell('cmd','locale','set-app-locales','com.magdi.tube','--locales',language);
  shell('am','start','-n','com.magdi.tube/.MainActivity');await wait(1500);
  shell('uiautomator','dump','/sdcard/tube-layout.xml');const xml=shell('cat','/sdcard/tube-layout.xml');
  const nodes=[...xml.matchAll(/<node\s+[^>]+/g)].map(m=>Object.fromEntries([...m[0].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],a[2]])));
  const buttons=labels[language].map(label=>{const b=nodes.find(n=>n.class==='android.widget.Button'&&n.text===label);assert(b,`Missing ${label}`);return {...b,rect:b.bounds.match(/\d+/g).map(Number)};});
  for(const b of buttons){const [l,t,r,d]=b.rect;assert(r-l>=96&&d-t>=96,`${b.text} touch target`);assert(l>=0&&r<=width*2,`${b.text} horizontal clipping`);assert.equal(b.enabled,b.text===labels[language][10]?'true':'false',`${b.text} disconnected state`);}
  if(scale===1){const groups=width>700?[buttons.slice(0,5),buttons.slice(5)]:[buttons.slice(0,5),buttons.slice(5,8),buttons.slice(8)];
   for(const row of groups)for(const b of row){assert.equal(b.rect[1],row[0].rect[1],`${b.text} top`);assert.equal(b.rect[3],row[0].rect[3],`${b.text} bottom`);}
  }
  for(let i=0;i<buttons.length;i++)for(let j=i+1;j<buttons.length;j++){const a=buttons[i].rect,b=buttons[j].rect;assert(a[2]<=b[0]||b[2]<=a[0]||a[3]<=b[1]||b[3]<=a[1],'Overlapping buttons');}
  writeFileSync(`${output}/${language}-${width}-${scale}.png`,adb('exec-out','screencap','-p'));
  console.log(`PASS real phone buttons: ${language}, ${width}dp, font ${scale}; aligned, non-overlapping, disabled without car`);
 }
}finally{
 shell('am','force-stop','com.magdi.tube');shell('wm','size',size);shell('wm','density',density);shell('settings','put','system','font_scale',font==='null'?'1':font);
 if(locale)shell('cmd','locale','set-app-locales','com.magdi.tube','--locales',locale);else shell('cmd','locale','set-app-locales','com.magdi.tube');
}
