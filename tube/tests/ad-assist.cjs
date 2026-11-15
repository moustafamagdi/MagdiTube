const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync('tube/src/main/assets/ad_assist.js','utf8');
function harness(host){let tick,removed=false,clicks=0;const buttons=[{disabled:false,getClientRects:()=>[1],click:()=>clicks++},{disabled:true,getClientRects:()=>[1],click:()=>{throw Error('disabled clicked')}},{disabled:false,getClientRects:()=>[],click:()=>{throw Error('hidden clicked')}}];const ctx={location:{protocol:'https:',hostname:host},window:{},document:{visibilityState:'visible',createElement:()=>({remove:()=>removed=true}),head:{appendChild:()=>{}},querySelectorAll:()=>buttons},setInterval:f=>(tick=f,1),clearInterval:()=>{tick=null}};vm.createContext(ctx);vm.runInContext(source,ctx);return {ctx,get tick(){return tick},get clicks(){return clicks},get removed(){return removed}};}
const google=harness('accounts.google.com');assert.equal(google.tick,undefined);
const fake=harness('youtube.com.evil.test');assert.equal(fake.tick,undefined);
const yt=harness('m.youtube.com');yt.tick();yt.tick();assert.equal(yt.clicks,1);assert.equal(yt.ctx.window.__magdiAds.stats().skipClicks,1);yt.ctx.window.__magdiAds.stop();assert.equal(yt.tick,null);assert(yt.removed);assert.equal(yt.ctx.window.__magdiAds,undefined);
console.log('Ad assist checks passed: domain isolation, enabled visible button only, no repeat clicks, disable cleanup.');
