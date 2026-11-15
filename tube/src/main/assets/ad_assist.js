(function(){
'use strict';
if(location.protocol!=='https:'||!(location.hostname==='youtube.com'||location.hostname.endsWith('.youtube.com')))return;
if(window.__magdiAds)return;
const css='ytm-promoted-sparkles-web-renderer,ytm-promoted-sparkles-text-search-renderer,ytm-ad-slot-renderer,ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-promoted-sparkles-web-renderer{display:none!important}';
const style=document.createElement('style');style.textContent=css;(document.head||document.documentElement).appendChild(style);
let clicks=0;
const seen=new WeakSet();
const timer=setInterval(function(){
if(document.visibilityState!=='visible')return;
for(const button of document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,ytm-skip-ad-button-renderer button')){
if(!seen.has(button)&&!button.disabled&&button.getClientRects().length){seen.add(button);button.click();clicks++;}
}
},700);
window.__magdiAds={stop:function(){clearInterval(timer);style.remove();delete window.__magdiAds;},stats:function(){return {skipClicks:clicks};}};
})();
