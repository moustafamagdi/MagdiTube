const assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const code=fs.readFileSync('tube/src/main/assets/car_controller.js','utf8');
function page(lease,hash=''){
 let tick,plays=0;
 const video={readyState:4,paused:true,ended:false,currentTime:12,duration:60,seekable:{length:1,start:()=>0,end:()=>60},play(){plays++;this.paused=false;return Promise.resolve();},pause(){this.paused=true;}};
 const location={href:'https://m.youtube.com/watch?v=AAAAAAAAAAA'+hash,hash};
 const window={__magdiCarLease:lease,__magdiArtwork:()=>({id:'AAAAAAAAAAA',title:'Fixture'})};
 vm.runInNewContext(code,{window,location,document:{querySelector:()=>video},URL,URLSearchParams,Number,setInterval:f=>{tick=f;return 1;},clearInterval(){},addEventListener(){}});
 return {window,video,location,tick:()=>tick(),plays:()=>plays};
}
// YouTube may remove the fragment before onPageFinished injects the helper.
const p=page('7_12');
assert.equal(p.window.__magdiCar.state().token,'7_12');
p.window.__magdiCar.start('AAAAAAAAAAA','7_12');p.tick();assert.equal(p.plays(),1);
assert.equal(p.window.__magdiCar.command('pause',0,'7_12'),true);assert.equal(p.video.paused,true);
assert.equal(p.window.__magdiCar.command('play',0,'7_11'),false);
// A URL fragment must never upgrade an old document to a new request.
const stale=page('7_11','#magdi_request=7_12');
assert.equal(stale.window.__magdiCar.state().token,'7_11');
stale.window.__magdiCar.start('AAAAAAAAAAA','7_12');stale.tick();assert.equal(stale.plays(),0);
const unbound=page(undefined,'#magdi_request=7_12');
assert.equal(unbound.window.__magdiCar.state().token,'');
console.log('PASS: native document lease survives fragment removal; stale documents and URL token forgery are rejected');
