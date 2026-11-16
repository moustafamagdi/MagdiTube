const assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
let qualityReads=0,artworkReads=0;
const events={};
const video={paused:false,ended:false,currentTime:12.4,readyState:4,muted:false,videoWidth:1920,videoHeight:1080,
 addEventListener:(name,fn)=>events[name]=fn,
 getVideoPlaybackQuality:()=>{qualityReads++;return {totalVideoFrames:600,droppedVideoFrames:4};}};
let current=video;
const ctx={window:{__magdiPlayback:{state:()=>({wanted:true})},__magdiArtwork:()=>{artworkReads++;return {id:'abcdefghijk',url:'https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg'};}},document:{querySelector:()=>current},WeakMap,JSON};
const script=fs.readFileSync('tube/src/main/assets/playback_sample.js','utf8');vm.runInNewContext(script,ctx);
const sample=(details,art)=>JSON.parse(ctx.window.__magdiSample(details,art));
let state=sample(false,false);assert.equal(state.playing,true);assert.equal(state.time,12);assert.equal(state.performance,undefined);assert.equal(qualityReads,0);assert.equal(artworkReads,0);
events.waiting();events.stalled();state=sample(true,true);assert.equal(state.performance.totalFrames,600);assert.equal(state.performance.droppedFrames,4);assert.equal(state.performance.width,1920);assert.equal(state.performance.waiting,1);assert.equal(state.performance.stalled,1);assert.equal(qualityReads,1);assert.equal(artworkReads,1);
for(let i=0;i<20;i++)sample(false,false);assert.equal(qualityReads,1);assert.equal(artworkReads,1);
events.loadstart();state=sample(true,false);assert.equal(state.performance.waiting,0);assert.equal(state.performance.loads,1);
video.getVideoPlaybackQuality=undefined;state=sample(true,false);assert.equal(state.performance.totalFrames,null);assert.equal(state.performance.droppedFrames,null);
current=null;state=sample(true,false);assert.equal(state.playing,false);assert.equal(state.performance.width,0);
// Re-injection cannot reset counters or attach duplicate listeners.
const helper=ctx.window.__magdiSample;vm.runInNewContext(script,ctx);assert.equal(ctx.window.__magdiSample,helper);
console.log('PASS: lightweight samples skip quality/artwork work; detailed frame counters, buffering events, resets and unsupported API fallback');
