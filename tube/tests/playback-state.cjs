const assert=require('assert'),fs=require('fs'),vm=require('vm');
function setup(){const handlers={},docHandlers={};let plays=0,queries=0,mutation;const v={paused:false,ended:false,isConnected:true,pause:()=>{v.paused=true;},addEventListener:(n,f)=>handlers[n]=f,play:()=>{plays++;return Promise.resolve();}};const document={hidden:false,documentElement:{},querySelector:()=>{queries++;return v;},querySelectorAll:()=>[v],addEventListener:(n,f)=>docHandlers[n]=f};const winHandlers={};const ctx={window:{addEventListener:(n,f)=>winHandlers[n]=f},document,MutationObserver:class{constructor(fn){mutation=fn;}observe(){}},Date};vm.createContext(ctx);vm.runInContext(fs.readFileSync('tube/src/main/assets/playback_state.js','utf8'),ctx);return {v,handlers,document,winHandlers,api:ctx.window.__magdiPlayback,docHandlers,plays:()=>plays,queries:()=>queries,mutate:()=>mutation()};}
let t=setup();assert.equal(t.api.cover(),true);t.v.paused=true;t.handlers.pause();assert.equal(t.api.resume(),'resume-requested');assert.equal(t.plays(),1);assert.equal(t.api.resume(),'no-resume');
t=setup();t.v.paused=true;t.handlers.pause();t.api.cover();assert.equal(t.api.resume(),'no-resume');
t=setup();t.api.cover();t.api.stop();t.v.paused=true;assert.equal(t.api.resume(),'no-resume');
t=setup();t.api.cover();t.v.ended=true;t.handlers.ended();assert.equal(t.api.resume(),'no-resume');
console.log('Resume intent tests passed: temporary cover, one attempt, manual pause, Stop and ended video.');

t=setup();t.winHandlers.blur();t.v.paused=true;t.handlers.pause();assert.equal(t.api.resume(),'resume-requested');console.log('Window blur before fullscreen pause preserves resume intent.');

t=setup();t.api.cover();t.api.suppress(true);assert.equal(t.v.paused,true);assert.equal(t.api.state().wanted,false);assert.equal(t.api.resume(),'no-resume');
t.v.paused=false;t.docHandlers.play({target:t.v});assert.equal(t.v.paused,true);assert.equal(t.plays(),0);
t.api.suppress(false);assert.equal(t.v.paused,true);assert.equal(t.api.resume(),'no-resume');assert.equal(t.plays(),0);
console.log('Suppressed phone blocks autoplay; releasing suppression does not restart media.');

t=setup();const initialQueries=t.queries();for(let i=0;i<1000;i++)t.mutate();assert.equal(t.queries(),initialQueries);
t.v.isConnected=false;t.mutate();assert.equal(t.queries(),initialQueries+1);
console.log('Connected video avoids repeated DOM searches during 1000 unrelated mutations.');

(async()=>{
 t=setup();t.api.interrupt(true);t.v.paused=true;t.handlers.pause();assert.equal(t.api.resume(),'no-resume');t.api.interrupt(false);assert.equal(t.api.resume(),'resume-requested');await Promise.resolve();
 t=setup();t.v.paused=true;t.handlers.pause();assert.equal(t.api.interrupt(true),true);t.api.interrupt(false);assert.equal(t.api.resume(),'resume-requested');
 t=setup();t.docHandlers.pointerdown();t.v.paused=true;t.handlers.pause();assert.equal(t.api.interrupt(true),false);t.api.interrupt(false);assert.equal(t.api.resume(),'no-resume');
 t=setup();t.api.interrupt(true);t.api.stop();t.v.paused=true;t.handlers.pause();t.api.interrupt(false);assert.equal(t.api.resume(),'no-resume');
 t=setup();t.api.interrupt(true);t.v.paused=true;t.handlers.pause();t.api.interrupt(false);let requests=0;t.v.play=()=>{requests++;return requests===1?Promise.reject(Error('focus busy')):Promise.resolve();};assert.equal(t.api.resume(),'resume-requested');await new Promise(r=>setImmediate(r));assert.equal(t.api.resume(),'resume-requested');assert.equal(requests,2);
 console.log('Call tests passed: early/late mode notice, no resume during call, manual pause/stop, rejected play retry.');
})().catch(e=>{console.error(e);process.exitCode=1});
