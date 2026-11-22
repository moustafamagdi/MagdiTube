const assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const code=fs.readFileSync('tube/src/main/assets/car_audio.js','utf8');
function fixture({phone=false,broken=false,api=true,paused=false}={}){
 let current,revoked=false,now=10000,interval,apiCalls=0,clicks=0,plays=0,apiMuted=true,shown=true;
 const listeners={},pending=[];
 const prompt={disabled:false,getClientRects:()=>shown?[{}]:[],click(){clicks++;if(!broken){shown=false;apiMuted=false;current.muted=false;}}};
 const player={querySelectorAll:()=>[current],querySelector:s=>s==='button.ytp-unmute,button.ytp-unmute-button'?prompt:null};
 if(api){player.isMuted=()=>apiMuted;player.getVolume=()=>current.volume*100;player.setVolume=x=>current.volume=x/100;
  player.unMute=()=>{apiCalls++;if(!broken){apiMuted=false;shown=false;current.muted=false;}};}
 function video(){return {paused,ended:false,error:null,muted:false,defaultMuted:true,volume:.35,isConnected:true,closest:()=>player,play(){plays++;this.paused=false;return Promise.resolve();}};}
 current=video();
 const window=phone?{}:{__magdiCar:{audioAllowed:v=>!revoked&&v===current&&v.isConnected}};
 const document={querySelector:()=>current,addEventListener:(n,f)=>listeners[n]=f,removeEventListener:n=>delete listeners[n]};
 vm.runInNewContext(code,{window,document,WeakMap,WeakSet,Date:{now:()=>now},getComputedStyle:()=>({display:shown?'block':'none',visibility:'visible',opacity:'1'}),setTimeout:f=>pending.push(f),setInterval:f=>{interval=f;return 1;},clearInterval:()=>interval=null,addEventListener(){}});
 return {window,player,prompt,get v(){return current;},state:()=>({apiCalls,clicks,plays,apiMuted,shown}),tick(){now+=1500;interval?.();while(pending.length)pending.shift()();},event(name){listeners[name]?.({type:name,target:current});while(pending.length)pending.shift()();},mute(){apiMuted=true;shown=true;current.muted=true;},replace(){const old=current;current=video();apiMuted=true;shown=true;return old;},revoke(){revoked=true;window.__magdiAudio.stop();}};
}
// Reproduces the real site: HTML video is audible, while YouTube isMuted and its prompt remain true.
const p=fixture();assert.equal(p.state().apiMuted,false);assert.equal(p.state().shown,false);assert.equal(p.state().apiCalls,1);assert.equal(p.state().clicks,0);assert.equal(p.v.volume,.35);
assert.equal(p.state().plays,0,'Audio repair must not start playback');
p.mute();p.tick();assert.equal(p.v.muted,false);assert.equal(p.state().apiMuted,false);
p.v.volume=0;p.tick();assert.equal(p.v.volume,.35,'Preserve last positive volume instead of forcing max');
p.v.paused=true;p.mute();p.tick();assert.equal(p.v.muted,true);assert.equal(p.state().plays,0);
p.v.paused=false;p.event('play');assert.equal(p.v.muted,false);
const old=p.replace();assert.equal(p.window.__magdiAudio.prepare(old),false);p.event('loadedmetadata');assert.equal(p.state().shown,false);
p.revoke();p.mute();p.tick();assert.equal(p.v.muted,true);assert.equal(p.window.__magdiAudio.prepare(p.v),false);
const fallback=fixture({api:false});assert.equal(fallback.state().clicks,1);assert.equal(fallback.state().shown,false);
const broken=fixture({broken:true});for(let i=0;i<20;i++)broken.tick();assert.equal(broken.state().apiCalls,4);assert.equal(broken.state().clicks,1,'No repeated clicks on an unresponsive prompt');
const paused=fixture({paused:true});assert.equal(paused.state().apiCalls,0);assert.equal(paused.state().plays,0);paused.window.__magdiAudio.prepare(paused.v);assert.equal(paused.state().apiMuted,false);assert.equal(paused.state().plays,0);
const phone=fixture({phone:true});assert.equal(phone.window.__magdiAudio,undefined);assert.equal(phone.state().apiCalls,0);assert.equal(phone.state().clicks,0);
console.log('PASS: car-only audio repair synchronizes YouTube state/prompt, preserves positive volume, handles new media, and bounds fallback without autoplay or stale callbacks');
