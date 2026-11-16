(function(){
 'use strict';
 // Read YouTube's published thumbnail only. Never sample video pixels or capture a view.
 window.__magdiArtwork=function(){
  try{
   const page=new URL(location.href);
   if(page.protocol!=='https:'||!['youtube.com','www.youtube.com','m.youtube.com'].includes(page.hostname))return null;
   const id=page.pathname==='/watch'?page.searchParams.get('v'):(page.pathname.match(/^\/(?:shorts|live)\/([\w-]{11})(?:\/|$)/)||[])[1];
   if(!id||! /^[\w-]{11}$/.test(id)||!document.querySelector('video'))return null;
   function valid(raw){try{const u=new URL(raw,page.href);return u.protocol==='https:'&&!u.username&&!u.password&&(!u.port||u.port==='443')&&['i.ytimg.com','img.youtube.com'].includes(u.hostname)&&new RegExp('^/vi(?:_webp)?/'+id+'/[a-zA-Z0-9_-]+\\.(?:jpg|webp)$').test(u.pathname)?u.href:null;}catch(e){return null;}}
   // Prefer the current player's response, which also detects a stale SPA transition.
   const player=document.getElementById('movie_player');
   let response=null;
   if(player&&typeof player.getPlayerResponse==='function'){
    try{response=player.getPlayerResponse();}catch(e){}
    if(response&&response.videoDetails&&response.videoDetails.videoId!==id)return null;
   }
   if(!response){const initial=window.ytInitialPlayerResponse;if(initial&&initial.videoDetails&&initial.videoDetails.videoId===id)response=initial;}
   const list=response&&response.videoDetails&&response.videoDetails.thumbnail&&response.videoDetails.thumbnail.thumbnails;
   if(Array.isArray(list)){
    const choices=list.filter(t=>valid(t.url)).sort((a,b)=>Math.abs((a.width||480)-480)-Math.abs((b.width||480)-480));
    if(choices.length)return {id:id,url:valid(choices[0].url)};
   }
   const poster=valid(document.querySelector('video').poster);
   if(poster)return {id:id,url:poster};
   const image=document.querySelector('meta[property="og:image"]');
   const meta=image&&valid(image.content);
   // The image path must contain this exact video's ID, even for cached page metadata.
   return {id:id,url:meta||('https://i.ytimg.com/vi/'+id+'/hqdefault.jpg')};
  }catch(e){return null;}
 };
})();
