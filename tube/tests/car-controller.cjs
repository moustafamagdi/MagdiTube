const assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const code=fs.readFileSync('tube/src/main/assets/car_controller.js','utf8');
function page(lease,hash=''){
 let tick,plays=0;
 const events={};
 const video={addEventListener(n,f){events[n]=f;},readyState:4,paused:true,ended:false,currentTime:12,duration:60,seekable:{length:1,start:()=>0,end:()=>60},play(){plays++;this.paused=false;return Promise.resolve();},pause(){this.paused=true;}};
 const location={href:'https://m.youtube.com/watch?v=AAAAAAAAAAA'+hash,hash};
 const window={__magdiCarLease:lease,__magdiCarDocument:"doc-1",__magdiArtwork:()=>({id:'AAAAAAAAAAA',title:'Fixture'})};
 vm.runInNewContext(code,{window,location,document:{querySelector:()=>video},URL,URLSearchParams,Number,setInterval:f=>{tick=f;return 1;},clearInterval(){},addEventListener(){}});
 return {window,video,location,events,tick:()=>tick(),plays:()=>plays};
}
// YouTube may remove the fragment before onPageFinished injects the helper.
const p=page('7_12');
assert.equal(p.window.__magdiCar.state().token,'7_12');
p.window.__magdiCar.start('AAAAAAAAAAA','7_12');p.tick();assert.equal(p.plays(),1);
assert.equal(p.window.__magdiCar.command('pause',0,'7_12','doc-1','AAAAAAAAAAA',1),true);assert.equal(p.video.paused,true);
assert.equal(p.window.__magdiCar.command('play',0,'7_11','doc-1','AAAAAAAAAAA',2),false);
// A URL fragment must never upgrade an old document to a new request.
const stale=page('7_11','#magdi_request=7_12');
assert.equal(stale.window.__magdiCar.state().token,'7_11');
stale.window.__magdiCar.start('AAAAAAAAAAA','7_12');stale.tick();assert.equal(stale.plays(),0);
const unbound=page(undefined,'#magdi_request=7_12');
assert.equal(unbound.window.__magdiCar.state().token,'');
console.log('PASS: native document lease survives fragment removal; stale documents and URL token forgery are rejected');

p.window.__magdiCar.start('AAAAAAAAAAA','7_12');p.tick();assert.equal(p.plays(),1);
// Queued actions cannot cross documents, player identity, revocation or a newer Pause.
assert.equal(p.window.__magdiCar.command('play',0,'7_12','doc-0','AAAAAAAAAAA',2),false);
assert.equal(p.window.__magdiCar.command('play',0,'7_12','doc-1','BBBBBBBBBBB',2),false);
assert.equal(p.window.__magdiCar.command('play',0,'','doc-1','AAAAAAAAAAA',2),false);
assert.equal(p.window.__magdiCar.guard('7_12','doc-1','AAAAAAAAAAA',0),false);
p.video.paused=false;p.video.readyState=2;assert.equal(p.window.__magdiCar.state().buffering,true);
p.video.readyState=4;p.events.waiting();assert.equal(p.window.__magdiCar.state().buffering,true);
p.events.playing();assert.equal(p.window.__magdiCar.state().buffering,false);
p.video.error={code:3};assert.equal(p.window.__magdiCar.state().error,true);assert.equal(p.window.__magdiCar.command('play',0,'7_12','doc-1','AAAAAAAAAAA',3),false);
p.video.error=null;p.video.ended=true;p.video.paused=true;assert.equal(p.window.__magdiCar.state().ended,true);assert.equal(p.window.__magdiCar.state().replay,true);
p.video.seekable.start=()=>10;assert.equal(p.window.__magdiCar.state().replay,false);assert.equal(p.window.__magdiCar.command('play',0,'7_12','doc-1','AAAAAAAAAAA',4),false);
p.window.__magdiCar.revoke();assert.equal(p.window.__magdiCar.guard('7_12','doc-1','AAAAAAAAAAA',5),false);
console.log('PASS: execution ownership, empty-token rejection, intent invalidation, buffering, media error and bounded replay support');

assert.equal(stale.window.__magdiCar.fullscreen(true,'7_12','doc-1','AAAAAAAAAAA',1),false);
assert.equal(p.window.__magdiCar.fullscreen(true,'7_12','doc-1','AAAAAAAAAAA',6),false);
console.log('PASS: remote fullscreen rejects obsolete and revoked document ownership before touching layout');
