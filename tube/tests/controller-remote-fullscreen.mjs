// Real Android WebViews; launch debug ControllerHarnessActivity and forward CDP to 9226.
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {writeFileSync} from 'node:fs';
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json();
const sockets=[],checks=[];
async function client(title){const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}};return async expression=>{const id=++serial;const result=await new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${expression}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;};}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(run,expression){for(let i=0;i<100;i++){try{if(await run('return '+expression))return;}catch{}await sleep(150);}throw Error('Timeout: '+expression);}
function pass(name){checks.push(name);console.log('PASS '+name);}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture');
 await until(phone,'JSON.parse(Harness.state()).connected&&window.__magdiPhoneEpoch>=0');
 await phone("document.querySelector('a').click()");
 await until(phone,"JSON.parse(Harness.state()).phase==='playing'&&JSON.parse(Harness.controls())['Car fullscreen']");
 // A second YouTube-style container must never be promoted above the active video.
 // A transformed/clipped ancestor and inline video dimensions must not constrain it.
 await car(`const p=document.querySelector('#player'),v=document.querySelector('video');
 p.style.cssText='transform:translateZ(0);overflow:hidden;contain:paint;width:180px;height:100px';
 v.style.cssText='width:160px;height:90px;left:5px;top:3px';
 const other=document.createElement('div');other.id='player-container-id';other.style.cssText='background:black;width:10px;height:10px';document.body.append(other);
 window.originalPlayerStyle=p.getAttribute('style');window.originalVideoStyle=v.getAttribute('style');window.originalVideo=v;window.originalParent=v.parentNode;window.originalDocument=__magdiCarDocument;`);
 await phone("Harness.command('fullscreen',0)");
 await until(car,"!!document.getElementById('magdi-car-fullscreen')");await sleep(800);
 const box=await car(`const v=document.querySelector('video'),r=v.getBoundingClientRect();return {width:r.width,height:r.height,vw:innerWidth,vh:innerHeight,top:document.elementFromPoint(innerWidth/2,innerHeight/2)?.tagName,playing:!v.paused}`);
 if(process.env.MM_TEST_ADB)writeFileSync('dist/remote-fullscreen-after.png',execFileSync(process.env.MM_TEST_ADB,['-s','emulator-5560','exec-out','screencap','-p']));
 console.log(JSON.stringify(box));assert.equal(box.top,'VIDEO','Fullscreen video must be unobscured, not covered by an unrelated black player container');
 assert.ok(Math.abs(box.width-box.vw)<2&&Math.abs(box.height-box.vh)<2,'Video must fill viewport despite transformed/clipped ancestors and inline dimensions');
 assert.ok(box.playing);pass('Remote fullscreen exposes the actual video across the viewport under hostile page layout');
 await phone("Harness.command('pause',0)");await until(phone,"JSON.parse(Harness.state()).phase==='paused'&&JSON.parse(Harness.controls()).Play&&!JSON.parse(Harness.controls()).Pause");
 assert.equal(await car("return document.elementFromPoint(innerWidth/2,innerHeight/2)===originalVideo&&originalVideo.paused"),true);
 await phone("Harness.command('play',0)");await until(phone,"JSON.parse(Harness.state()).phase==='playing'");pass('Phone Pause/Play stay usable while remote fullscreen is visible');
 const time=await car('return originalVideo.currentTime');await sleep(1200);
 assert.equal(await car(`return originalVideo===document.querySelector('video')&&originalVideo.parentNode===originalParent&&__magdiCarDocument===originalDocument&&!originalVideo.paused&&originalVideo.currentTime>${time}`),true);pass('Fullscreen preserves the same video, DOM parent, document and advancing playback');
 await phone('Harness.coverCar(true)');await sleep(1500);await phone('Harness.coverCar(false)');await sleep(700);
 assert.equal(await car("return document.elementFromPoint(innerWidth/2,innerHeight/2)===originalVideo&&!originalVideo.paused"),true);pass('Remote fullscreen remains visible after cover and return');
 await phone("Harness.command('fullscreen',0)");await until(car,"!document.getElementById('magdi-car-fullscreen')");
 assert.equal(await car("return document.querySelector('#player').getAttribute('style')===originalPlayerStyle&&originalVideo.getAttribute('style')===originalVideoStyle&&!document.querySelector('[data-mm-fullscreen]')"),true);pass('Exit restores original page styles without replacing the video');
 await car("originalVideo.style.setProperty('display','none','important')");
 await phone("Harness.command('fullscreen',0)");await sleep(800);
 assert.equal(await car("return !document.getElementById('magdi-car-fullscreen')&&!document.querySelector('[data-mm-fullscreen]')"),true);
 await car("originalVideo.setAttribute('style',originalVideoStyle)");pass('A video that cannot be exposed rejects fullscreen and removes temporary styles');
 await phone("Harness.command('fullscreen',0);Harness.command('fullscreen',0)");await sleep(900);
 assert.equal(await car("return !document.getElementById('magdi-car-fullscreen')"),true);pass('Rapid fullscreen enter/exit leaves no late overlay');
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");
 await phone("document.querySelectorAll('a')[1].click()");await until(phone,"JSON.parse(Harness.state()).phase==='playing'&&JSON.parse(Harness.state()).playback.title==='Fixture BBBBBBBBBBB'");
 assert.equal(await car("return !document.getElementById('magdi-car-fullscreen')"),true);pass('Replacement selection cancels fullscreen without affecting the new document');
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");
 await phone("Harness.command('stop',0)");await until(car,"location.pathname==='/'&&!document.querySelector('video')");
 assert.equal(await car("return !document.getElementById('magdi-car-fullscreen')"),true);pass('Stop from remote fullscreen returns Home');
 console.log(JSON.stringify({passed:checks.length,checks},null,2));
}finally{for(const ws of sockets)ws.close();}
