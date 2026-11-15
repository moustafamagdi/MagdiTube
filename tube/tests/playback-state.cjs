const assert=require('assert'),fs=require('fs'),vm=require('vm');
function setup(){const handlers={},docHandlers={};let plays=0;const v={paused:false,ended:false,addEventListener:(n,f)=>handlers[n]=f,play:()=>{plays++;return Promise.resolve();}};const document={hidden:false,documentElement:{},querySelector:()=>v,addEventListener:(n,f)=>docHandlers[n]=f};const winHandlers={};const ctx={window:{addEventListener:(n,f)=>winHandlers[n]=f},document,MutationObserver:class{observe(){}},Date};vm.createContext(ctx);vm.runInContext(fs.readFileSync('tube/src/main/assets/playback_state.js','utf8'),ctx);return {v,handlers,document,winHandlers,api:ctx.window.__magdiPlayback,plays:()=>plays};}
let t=setup();assert.equal(t.api.cover(),true);t.v.paused=true;t.handlers.pause();assert.equal(t.api.resume(),'resume-requested');assert.equal(t.plays(),1);assert.equal(t.api.resume(),'no-resume');
t=setup();t.v.paused=true;t.handlers.pause();t.api.cover();assert.equal(t.api.resume(),'no-resume');
t=setup();t.api.cover();t.api.stop();t.v.paused=true;assert.equal(t.api.resume(),'no-resume');
t=setup();t.api.cover();t.v.ended=true;t.handlers.ended();assert.equal(t.api.resume(),'no-resume');
console.log('Resume intent tests passed: temporary cover, one attempt, manual pause, Stop and ended video.');

t=setup();t.winHandlers.blur();t.v.paused=true;t.handlers.pause();assert.equal(t.api.resume(),'resume-requested');console.log('Window blur before fullscreen pause preserves resume intent.');
