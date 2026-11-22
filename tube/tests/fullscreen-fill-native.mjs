// Synthetic media in real Android WebViews. Launch the app's debug ControllerHarnessActivity.
// TEST_APP=tube uses CDP 9226; TEST_APP=iptv uses CDP 9225 and the local mock server.
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {writeFileSync,mkdirSync} from 'node:fs';
const tube=process.env.TEST_APP==='tube',port=tube?9226:9225;
const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json(),sockets=[];
async function client(phone){
 const p=pages.find(p=>tube?p.title===(phone?'Phone Controller':'Car Fixture'):p.type==='page'&&p.title.includes('Controller')===phone);assert(p,'Missing fixture page');
 const ws=new WebSocket(p.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();
 ws.onmessage=e=>{const m=JSON.parse(e.data),p=pending.get(m.id);if(p){pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result);}};
 return async code=>{const id=++serial;const r=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(id);reject(Error('CDP timeout'));},20000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${code}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;};
}
const wait=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,expr){for(let i=0;i<120;i++){if(await fn('return '+expr))return;await wait(100);}throw Error('Timeout: '+expr);}
const pass=s=>console.log('PASS '+s);
function screenshot(mode){if(!process.env.SCREENSHOT_DIR)return;mkdirSync(process.env.SCREENSHOT_DIR,{recursive:true});writeFileSync(`${process.env.SCREENSHOT_DIR}/${tube?'tube':'iptv'}-${mode}.png`,execFileSync(process.env.ADB||'adb',['-P',process.env.ADB_PORT||'5038','-s',process.env.ADB_DEVICE||'emulator-5560','exec-out','screencap','-p'],{maxBuffer:32*1024*1024}));}
try{
 const phone=await client(true),car=await client(false);
 if(!tube){await phone("language='en';applyLanguage()");await car("language='en';applyLanguage()");}
 const button=tube?'JSON.parse(Harness.fitButton())':"({enabled:!$('controller-fit').disabled,text:$('controller-fit').textContent})";
 const tap=tube?'Harness.tapFit()':"$('controller-fit').click()";
 const full=tube?"Harness.command('fullscreen',0)":"$('controller-full').click()";
 const cmd=a=>tube?`Harness.command('${a}',0)`:`$('controller-${a}').click()`;
 if(tube){await until(phone,'JSON.parse(Harness.state()).connected');await phone("document.querySelector('a').click()");}
 else await phone("await api('save',{mode:'xtream',host:'http://10.0.2.2:8787',username:'demo',password:'demo'});account=await api('config');await connectAccount();await changeTab('series');await openSeries(filtered[0]);document.querySelector('#episodes .pick').click();");
 await until(car,"document.querySelector('video')?.readyState>=3&&!document.querySelector('video').paused");
 await until(phone,tube?"JSON.parse(Harness.controls()).Pause":"!$('controller-pause').disabled");
 assert.equal((await phone('return '+button)).enabled,false);pass('Actual phone Fill button disabled outside fullscreen');
 await car("window.fitVideo=document.querySelector('video');window.fitSource=fitVideo.currentSrc;window.fitMuted=fitVideo.muted;");
 await phone(full);await until(phone,button+'.enabled');
 for(const [w,h] of [[960,540],[1280,480],[480,800]]){
  await phone(tube?`Harness.viewport(${w},${h})`:`Harness.setCarViewport(${w},${h})`);
  await until(car,`innerWidth===${w}&&innerHeight===${h}`);
  await phone(tap);await until(car,"MMVideoFit.state().fill&&getComputedStyle(fitVideo).objectFit==='cover'");
  await until(phone,button+".text==='Full picture'");
  assert(await car("return fitVideo===document.querySelector('video')&&fitVideo.currentSrc===fitSource&&fitVideo.muted===fitMuted&&!fitVideo.paused"));
  const dims=await car('const r=fitVideo.getBoundingClientRect();return {w:r.width,h:r.height,vw:innerWidth,vh:innerHeight}');assert(Math.abs(dims.w-dims.vw)<2&&Math.abs(dims.h-dims.vh)<2,JSON.stringify(dims));
  if(w===1280)screenshot('fill');
  await phone(tap);await until(car,"!MMVideoFit.state().fill&&getComputedStyle(fitVideo).objectFit==='contain'");await until(phone,button+".text==='Fill screen'");pass(`${w}x${h}: actual phone buttons switch cover/contain without replacing or pausing video`);
  if(w===1280)screenshot('fit');
 }
 await phone(cmd('pause'));await until(car,'fitVideo.paused');await wait(350);const pausedTime=await car('return fitVideo.currentTime');
 await phone(tap);await until(car,'MMVideoFit.state().fill');assert(await car(`return fitVideo.paused&&Math.abs(fitVideo.currentTime-${pausedTime})<.15`));
 await until(phone,button+".text==='Full picture'");if(tube)await phone('Harness.tapCarFit()');else await car("$('picture-fit').click()");await until(car,'!MMVideoFit.state().fill');await until(phone,button+".text==='Fill screen'");pass('Car-local Fit button and paused toggling preserve playback position');
 await phone(cmd('play'));await until(car,'!fitVideo.paused');const frames=await car('return fitVideo.getVideoPlaybackQuality().totalVideoFrames');await wait(700);assert(await car(`return fitVideo.getVideoPlaybackQuality().totalVideoFrames>${frames}`));pass('Decoder continues producing frames after mode changes');
 await phone(full);await until(car,'!MMVideoFit.state().fullscreen');await until(phone,'!'+button+'.enabled');
 await car('await fitVideo.requestFullscreen()');await until(car,'!!document.fullscreenElement');await until(phone,button+'.enabled');await phone(tap);await until(car,"MMVideoFit.state().fill&&getComputedStyle(fitVideo).objectFit==='cover'");
 await until(phone,button+".text==='Full picture'");await phone(tap);await until(car,"!MMVideoFit.state().fill&&getComputedStyle(fitVideo).objectFit==='contain'");await car('await document.exitFullscreen()');await until(phone,'!'+button+'.enabled');pass('Native WebView fullscreen supports both modes and exit disables the phone button');
 assert(await phone(tube?"return !document.querySelector('video')&&Harness.muted()==='true'":"return v.paused&&!v.getAttribute('src')&&v.getVideoPlaybackQuality().totalVideoFrames===0"));pass('No phone media playback');
 await phone(cmd('stop'));await until(phone,'!'+button+'.enabled');pass('Stop disables Fill and removes the fullscreen player');
 console.log('PASS fullscreen fit/fill native regression: '+(tube?'Tube':'IPTV'));
}finally{for(const ws of sockets)ws.close();}
