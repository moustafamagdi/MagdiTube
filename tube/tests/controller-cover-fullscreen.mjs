// Real Android WebViews; launch debug ControllerHarnessActivity and forward CDP to 9226.
import assert from 'node:assert/strict';
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json();
const sockets=[],checks=[];
async function client(title){const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}};return async expression=>{const id=++serial;const result=await new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${expression}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;};}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(run,expression){for(let i=0;i<100;i++){try{if(await run('return '+expression))return;}catch{}await sleep(150);}throw Error('Timeout: '+expression);}
function pass(name){checks.push(name);console.log('PASS '+name);}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture');
 await car("document.querySelector('video').currentTime=3;await document.querySelector('video').play();await document.querySelector('video').requestFullscreen()");await until(car,'!!document.fullscreenElement');
 const r=await phone('return JSON.parse(Harness.state()).request'),doc=await car('return window.__magdiCarDocument');
 await phone('Harness.coverCar(true)');await sleep(2500);assert.equal(await car("return !!document.fullscreenElement&&!document.querySelector('video').paused&&document.querySelector('video').currentTime>4"),true);
 await phone('Harness.coverCar(false)');await sleep(1500);assert.equal(await car('return !!document.fullscreenElement&&window.__magdiCarDocument'),doc);assert.equal(await phone('return JSON.parse(Harness.state()).request'),r);pass('Native fullscreen keeps advancing during cover and preserves document/request on return');
 await car('await document.exitFullscreen()');
}finally{for(const ws of sockets)ws.close();}
