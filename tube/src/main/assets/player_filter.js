(function(){
'use strict';
if(location.protocol!=='https:'||!['youtube.com','www.youtube.com','m.youtube.com'].includes(location.hostname)||window.__magdiPlayerFilter)return;
let active=true,removed=0,nonEmpty=0;
const parse=JSON.parse;
function clean(value){
 if(!active||!value||typeof value!=='object')return value;
 // Only recognized player responses, never arbitrary account or browse JSON.
 const player=value.playerResponse||value;
 if(player&&player.videoDetails&&player.playabilityStatus){
 for(const key of ['adPlacements','adSlots','playerAds']){
 if(Object.prototype.hasOwnProperty.call(player,key)){
 const field=player[key];
 if(field&&(typeof field!=='object'||Object.keys(field).length>0))nonEmpty++;
 delete player[key];removed++;
 }
 }
 }
 return value;
}
const wrappedParse=function(){return clean(Reflect.apply(parse,this,arguments));};
JSON.parse=wrappedParse;
let responseJson,wrappedResponse;
if(typeof Response!=='undefined'){
 responseJson=Response.prototype.json;
 wrappedResponse=function(){return Reflect.apply(responseJson,this,arguments).then(clean);};
 Response.prototype.json=wrappedResponse;
}
const old=Object.getOwnPropertyDescriptor(window,'ytInitialPlayerResponse');
let initial=window.ytInitialPlayerResponse;
let hooked=false;
if(!old||old.configurable){
 Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,enumerable:true,get:()=>initial,set:value=>{initial=clean(value);}});hooked=true;
}
clean(initial);
window.__magdiPlayerFilter={stats:()=>({removed:removed,nonEmpty:nonEmpty}),stop:()=>{
 active=false;if(JSON.parse===wrappedParse)JSON.parse=parse;
 if(wrappedResponse&&Response.prototype.json===wrappedResponse)Response.prototype.json=responseJson;
 if(hooked)Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,enumerable:true,writable:true,value:initial});
 delete window.__magdiPlayerFilter;
}};
})();
