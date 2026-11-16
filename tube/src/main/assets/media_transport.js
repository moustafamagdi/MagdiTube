(function(){
 'use strict';
 // Use the current player's enabled controls or a verified playlist neighbour.
 // Never use browser history, arbitrary recommendations, or seek as "previous".
 function context(id){
  const page=new URL(location.href);
  if(page.protocol!=='https:'||!['youtube.com','m.youtube.com','www.youtube.com'].includes(page.hostname)||page.pathname!=='/watch'||page.searchParams.get('v')!==id)return null;
  const player=document.getElementById('movie_player');
  if(!player||!document.querySelector('video'))return null;
  let current='';
  try{const r=typeof player.getPlayerResponse==='function'&&player.getPlayerResponse();current=r&&r.videoDetails&&r.videoDetails.videoId;
   if(!current&&typeof player.getVideoData==='function')current=player.getVideoData().video_id;
  }catch(e){return null;}
  return current===id?player:null;
 }
 function action(player,next,id){
  const selector=next?'.ytp-next-button':'.ytp-prev-button';
  const button=player.querySelector(selector);
  if(button&&!button.disabled&&button.getAttribute('aria-disabled')!=='true'&&!button.hasAttribute('disabled')){
   const style=getComputedStyle(button);
   // Visibility/opacity can hide auto-fading controls; display:none means unavailable.
   if(style.display!=='none'&&typeof button.click==='function')return ()=>button.click();
  }
  const method=next?'nextVideo':'previousVideo';
  if(typeof player[method]!=='function'||typeof player.getPlaylist!=='function'||typeof player.getPlaylistIndex!=='function')return null;
  const list=player.getPlaylist(),index=player.getPlaylistIndex(),neighbour=index+(next?1:-1);
  if(!Array.isArray(list)||!Number.isInteger(index)||index<0||index>=list.length||list[index]!==id||neighbour<0||neighbour>=list.length||! /^[\w-]{11}$/.test(list[neighbour]))return null;
  return ()=>player[method]();
 }
 window.__magdiTransport={
  state:id=>{try{const p=context(id);return {next:!!p&&!!action(p,true,id),previous:!!p&&!!action(p,false,id)};}catch(e){return {next:false,previous:false};}},
  skip:(id,next)=>{try{const p=context(id),run=p&&action(p,!!next,id);if(!run)return 'unavailable';run();return 'requested';}catch(e){return 'unavailable';}}
 };
})();
