// API 36 debug harness; synthetic media, actual Chromium decoding and MediaSession callbacks.
// Start ControllerHarnessActivity fresh and forward its WebView CDP to port 9226.
import assert from 'node:assert/strict';
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json(),sockets=[];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));let passed=0;
async function client(title){
 const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);
 const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);
 await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let seq=0;const pending=new Map();
 ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}};
 return async expression=>{const id=++seq;const result=await new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${expression}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;};
}
async function until(run,expression){for(let i=0;i<100;i++){if(await run('return '+expression))return;await sleep(150);}throw Error('Timeout: '+expression);}
function pass(name){console.log('PASS '+name);passed++;}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture');
 await until(phone,'JSON.parse(Harness.state()).connected');
 assert.equal(await phone("return Harness.audioCard('status')"),'unavailable');
 await phone("document.querySelector('a').click()");
 await until(car,"document.querySelector('video')?.currentTime>0.5");
 await until(phone,"Harness.audioCard('status')==='sent'");
 pass('Car playback automatically creates the native MediaSession without opening Audio');
 await phone("Harness.audioCard('pause')");await until(car,"document.querySelector('video').paused");
 await until(phone,'JSON.parse(Harness.controls()).Play&&!JSON.parse(Harness.controls()).Pause');
 await phone("Harness.audioCard('play')");await until(car,"!document.querySelector('video').paused");
 await until(phone,'JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play');
 pass('Real MediaSession Pause/Play changes car playback and native phone buttons');
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");
 await phone('Harness.coverCar(true)');const before=await car("return document.querySelector('video').currentTime");
 await sleep(1800);assert.equal(await car(`return document.querySelector('video').currentTime>${before}&&!document.querySelector('video').paused`),true);
 await phone("Harness.audioCard('pause')");await until(car,"document.querySelector('video').paused");
 await phone('Harness.coverCar(false)');assert.equal(await car("return document.querySelector('video').paused&&!!document.getElementById('magdi-car-fullscreen')"),true);
 await phone("Harness.audioCard('play')");await until(car,"!document.querySelector('video').paused");
 pass('Covered fullscreen keeps decoding and card commands; returning preserves Pause/fullscreen');
 await phone('Harness.closePhone()');const time=await car("return document.querySelector('video').currentTime");await sleep(1000);
 assert.equal(await car(`return !document.querySelector('video').paused&&document.querySelector('video').currentTime>${time}`),true);
 assert.equal(await phone("return !document.querySelector('video')&&Harness.muted()==='true'"),true);
 pass('Closing phone preserves car playback and phone never plays media');
 await phone("Harness.audioCard('stop')");await until(car,"!document.querySelector('video')");await sleep(1500);
 assert.equal(await phone("return Harness.audioCard('status')"),'unavailable');
 pass('MediaSession Stop cancels playback and releases the automatic binding');
 await phone('Harness.toggleCar()');await phone('Harness.toggleCar()');await sleep(1200);
 assert.equal(await car("return !document.querySelector('video')"),true);
 assert.equal(await phone("return Harness.audioCard('status')"),'unavailable');
 pass('Reconnection does not recreate playback or the audio service');
 console.log(JSON.stringify({passed,synthetic:true,audibleOutputTested:false,physicalAndroidAutoTested:false}));
}finally{for(const ws of sockets)ws.close();}
