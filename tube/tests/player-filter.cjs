const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync('tube/src/main/assets/player_filter.js','utf8');
function context(host){const c={location:{protocol:'https:',hostname:host},window:{}};vm.createContext(c);vm.runInContext(source,c);return c;}
const c=context('m.youtube.com');
assert.equal(vm.runInContext('JSON.parse(\'{"videoDetails":{"videoId":"test"},"playabilityStatus":{"status":"OK"},"adPlacements":[1],"streamingData":{"formats":[1]}}\').adPlacements',c),undefined);
assert.equal(vm.runInContext('JSON.parse(\'{"account":"test","adSlots":[1]}\').adSlots.length',c),1);
assert.equal(vm.runInContext('JSON.parse(\'{"videoDetails":{},"playabilityStatus":{},"streamingData":{"formats":[1]}}\').streamingData.formats.length',c),1);
vm.runInContext('window.__magdiPlayerFilter.stop()',c);
assert.equal(vm.runInContext('JSON.parse(\'{"videoDetails":{},"playabilityStatus":{},"adSlots":[1]}\').adSlots.length',c),1);
assert.equal(context('accounts.google.com').window.__magdiPlayerFilter,undefined);
assert.equal(context('youtube.com.evil.test').window.__magdiPlayerFilter,undefined);
console.log('Player filter passed: player-only removal, stream/account preservation, disable restoration, origin isolation');

const initial=context('www.youtube.com');
vm.runInContext('window.ytInitialPlayerResponse={videoDetails:{},playabilityStatus:{},adSlots:[1],streamingData:{token:"unchanged"}}',initial);
assert.equal(initial.window.ytInitialPlayerResponse.adSlots,undefined);
assert.equal(initial.window.ytInitialPlayerResponse.streamingData.token,'unchanged');
assert.throws(()=>vm.runInContext('JSON.parse("invalid")',initial));
assert.equal(vm.runInContext(`JSON.parse('{"n":2}', (k,v)=>k==="n"?3:v).n`,initial),3);
const asyncContext={location:{protocol:'https:',hostname:'m.youtube.com'},window:{}};
vm.createContext(asyncContext);
vm.runInContext('class Response {json(){return Promise.resolve({playerResponse:{videoDetails:{},playabilityStatus:{},playerAds:[1],streamingData:{formats:[1]}}});}}',asyncContext);
vm.runInContext(source,asyncContext);
vm.runInContext('new Response().json()',asyncContext).then(result=>{assert.equal(result.playerResponse.playerAds,undefined);assert.equal(result.playerResponse.streamingData.formats.length,1);console.log('Initial assignment, reviver, parse errors and Response.json tests passed');}).catch(e=>{console.error(e);process.exitCode=1;});
