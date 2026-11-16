const assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const script=fs.readFileSync('tube/src/main/assets/media_artwork.js','utf8');
const id='abcdefghijk',other='12345678901';
function page({path='/watch?v='+id,response,initial,poster='',meta='',host='m.youtube.com',video=true}={}){
 const c={URL,location:{href:'https://'+host+path},window:{ytInitialPlayerResponse:initial},document:{
  getElementById:()=>response?{getPlayerResponse:()=>response}:null,
  querySelector:s=>s==='video'?(video?{poster}:null):(meta?{content:meta}:null)
 }};vm.runInNewContext(script,c);return JSON.parse(JSON.stringify(c.window.__magdiArtwork()));
}
const url=(v=id,name='hqdefault')=>'https://i.ytimg.com/vi/'+v+'/'+name+'.jpg';
assert.equal(page({response:{videoDetails:{videoId:id,thumbnail:{thumbnails:[{url:url(id,'maxresdefault'),width:1280},{url:url(),width:480}]}}}}).url,url());
assert.equal(page({response:{videoDetails:{videoId:other}}}),null,'stale player response during SPA navigation must not label the new video');
assert.equal(page({initial:{videoDetails:{videoId:other}},meta:url(other)}).url,url(),'stale initial response/meta cannot supply another video artwork');
assert.equal(page({poster:url(id,'mqdefault')}).url,url(id,'mqdefault'));
assert.equal(page({meta:'https://i.ytimg.com.evil.test/vi/'+id+'/hqdefault.jpg'}).url,url());
assert.equal(page({meta:'data:image/jpeg;base64,frame'}).url,url());
assert.equal(page({path:'/results?search_query=example'}),null);
assert.equal(page({video:false}),null);
assert.equal(page({host:'youtube.com.evil.test'}),null);
assert.equal(page({path:'/shorts/'+id}).id,id);
assert.equal(page({path:'/live/'+id}).id,id);
assert.equal(page({path:'/watch?v=invalid'}),null);
// The harness deliberately has no canvas, frame callback, capture API, fetch, or video pixel access.
console.log('PASS: 12 thumbnail source/identity checks; no video capture APIs used');
const titled=page({response:{videoDetails:{videoId:id,title:'  Video\nTitle  ',author:'Channel'}}});
assert.equal(titled.title,'Video Title');assert.equal(titled.author,'Channel');
assert.equal(page({initial:{videoDetails:{videoId:other,title:'Wrong title',author:'Wrong channel'}}}).title,'');
assert.equal(page({response:{videoDetails:{videoId:id,title:'x'.repeat(500)}}}).title.length,300);
assert.equal(page({response:{videoDetails:{videoId:id,title:{bad:'object'}}}}).title,'');
console.log('PASS: current identity only, bounded text and no stale title fallback');
