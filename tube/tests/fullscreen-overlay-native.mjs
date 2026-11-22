// API 36 debug harness + synthetic video. Real Android input, not JS button clicks.
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {writeFileSync,mkdirSync} from 'node:fs';
const adb=process.env.ADB||'adb',prefix=['-P',process.env.ADB_PORT||'5038','-s',process.env.ADB_DEVICE||'emulator-5560'];
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json(),sockets=[];
async function client(title){const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data),p=pending.get(m.id);if(p){pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result);}};return async code=>{const id=++serial;const r=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('CDP timeout')),20000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${code}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;};}
const wait=ms=>new Promise(r=>setTimeout(r,ms));
async function until(fn,expr){for(let i=0;i<120;i++){if(await fn('return '+expr))return;await wait(100);}throw Error('Timeout '+expr);}
function tap(b){execFileSync(adb,[...prefix,'shell','input','tap',String(b.x),String(b.y)]);}
function shot(name){if(!process.env.SCREENSHOT_DIR)return;mkdirSync(process.env.SCREENSHOT_DIR,{recursive:true});writeFileSync(`${process.env.SCREENSHOT_DIR}/${name}.png`,execFileSync(adb,[...prefix,'exec-out','screencap','-p'],{maxBuffer:32*1024*1024}));}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture');
 await until(phone,'JSON.parse(Harness.state()).connected');await phone('Harness.viewport(960,540)');await phone("document.querySelector('a').click()");await until(car,"document.querySelector('video')?.currentTime>.2");await until(phone,"JSON.parse(Harness.controls())['Car fullscreen']");
 await car("window.originalVideo=document.querySelector('video');window.originalSource=originalVideo.currentSrc;window.pointerEvents=0;document.addEventListener('pointerdown',()=>pointerEvents++);");
 for(const mode of ['remote','native']){
  if(mode==='remote')await phone("Harness.command('fullscreen',0)");else await car('await originalVideo.requestFullscreen()');
  await until(phone,'!!JSON.parse(Harness.overlay()).fit?.visible');
  let buttons=await phone('return JSON.parse(Harness.overlay())');
  for(const b of Object.values(buttons)){assert(b.image);assert.equal(b.width,b.height);assert(b.width>=48);}
  shot(mode+'-shown');console.log('PASS '+mode+': both controls are circular-size image buttons');
  await wait(5500);buttons=await phone('return JSON.parse(Harness.overlay())');assert(!buttons.exit.visible&&!buttons.fit.visible);shot(mode+'-hidden');
  const pointerCount=await car('return pointerEvents');tap(buttons.fit);await until(phone,'JSON.parse(Harness.overlay()).fit.visible');assert.equal(await car('return MMVideoFit.state().fill'),false);assert.equal(await car('return originalVideo.paused'),false);if(mode==='remote')assert.equal(await car('return pointerEvents'),pointerCount);
  console.log('PASS '+mode+': five-second hide and first tap reveal without clicking hidden Fit or pausing');
  tap(buttons.fit);await until(car,'MMVideoFit.state().fill');await until(phone,'JSON.parse(Harness.overlay()).fit.selected');assert((await phone('return JSON.parse(Harness.overlay()).fit.description')).includes('full picture'));
  const frames=await car('return originalVideo.getVideoPlaybackQuality().totalVideoFrames');await wait(5500);assert(await car(`return originalVideo.getVideoPlaybackQuality().totalVideoFrames>${frames}`));assert.equal(await phone('return JSON.parse(Harness.overlay()).fit.visible'),false);
  await phone("Harness.command('fill',0)");await until(car,'!MMVideoFit.state().fill');await until(phone,'!JSON.parse(Harness.overlay()).fit.selected');assert.equal(await phone('return JSON.parse(Harness.overlay()).fit.visible'),false);
  console.log('PASS '+mode+': icon tracks mode, decoded frames advance, remote changes do not reveal overlays');
  buttons=await phone('return JSON.parse(Harness.overlay())');tap(buttons.exit);await until(phone,'JSON.parse(Harness.overlay()).exit.visible');assert(await car('return MMVideoFit.state().fullscreen'));
  tap(buttons.exit);await until(car,'!MMVideoFit.state().fullscreen');await until(phone,'!JSON.parse(Harness.overlay()).exit');assert(await car('return originalVideo===document.querySelector("video")&&originalVideo.currentSrc===originalSource&&!originalVideo.paused'));
  console.log('PASS '+mode+': hidden Exit only reveals; second tap exits without stopping playback');
 }
 await phone("Harness.command('fullscreen',0)");await until(phone,'!!JSON.parse(Harness.overlay()).exit');await phone("Harness.command('stop',0)");await until(phone,'!JSON.parse(Harness.overlay()).exit');await wait(5500);assert.equal(await phone('return !!JSON.parse(Harness.overlay()).exit'),false);
 assert(await phone("return !document.querySelector('video')&&Harness.muted()==='true'"));console.log('PASS Stop clears overlay timers; phone remains a silent controller');
}finally{for(const ws of sockets)ws.close();}
