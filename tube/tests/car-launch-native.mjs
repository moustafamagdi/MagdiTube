// Requires an API 36 fixture emulator WITHOUT Android Auto installed. Tests failure handling, not launch compatibility.
import {execFileSync} from 'node:child_process';
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
 const phone=await client('Phone Controller'),car=await client('Car Fixture');await until(phone,'JSON.parse(Harness.state()).connected');
 const before=await phone('return Harness.state()');
 const adb=(...args)=>execFileSync(process.env.ADB,['-P','5038','-s','emulator-5560',...args],{encoding:'utf8'});
 const dump=()=>{adb('shell','uiautomator','dump','/sdcard/mm-launch-test.xml');return adb('shell','cat','/sdcard/mm-launch-test.xml');};
 let xml=dump(),node=xml.match(/<node[^>]*text="Open on car"[^>]*>/)?.[0];assert(node,'Native launch button missing');let b=node.match(/bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/).slice(1).map(Number);
 adb('shell','input','tap',String((b[0]+b[2])>>1),String((b[1]+b[3])>>1));await sleep(8500);xml=dump();
 assert(xml.includes('Could not confirm opening.'));assert(/text="Open on car"[^>]*enabled="true"/.test(xml));pass('Actual native Open on car button reports unsupported host and becomes retryable');
 assert.equal(await car("return !document.querySelector('video')"),true);assert.equal((await phone('return JSON.parse(Harness.state()).request')),JSON.parse(before).request);pass('A launch failure neither selects nor starts media');
 await phone('Harness.toggleCar()');await phone("document.querySelector('a').click()");await sleep(8500);await phone('Harness.toggleCar()');await sleep(1500);
 assert.equal(await car("return !document.querySelector('video')"),true);assert.equal(await phone("return Harness.muted()==='true'&&!document.querySelector('video')"),true);pass('Unavailable selection is discarded across failed launch and reconnection; phone stays silent');
 console.log(JSON.stringify({passed,actualAndroidAutoLaunchTested:false}));
}finally{for(const ws of sockets)ws.close();}
