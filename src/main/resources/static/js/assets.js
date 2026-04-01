(function(){ if(!window.App.ensureLoggedIn()) return; window.App.renderNav('assets.html');
let all=[];
function mask(v){ if(!v) return ''; return v.slice(0,4)+'••••••'+v.slice(-4); }
function render(rows){ document.getElementById('assetsBody').innerHTML=rows.map(a=>`<tr><td>${a.assetName}</td><td>${a.assetTag}</td><td>${a.assetType}</td><td>${a.status}</td><td>${a.assignedDepartmentId||''}</td><td>${a.assignedCompanyId||''}</td><td>${mask(a.deviceKey)}</td></tr>`).join(''); }
function applyFilter(){const q=(assetSearch.value||'').toLowerCase();const t=assetTypeFilter.value;render(all.filter(a=>(!t||a.assetType===t)&&(`${a.assetName} ${a.assetTag}`.toLowerCase().includes(q))));}
assetSearch.oninput=applyFilter; assetTypeFilter.onchange=applyFilter;
async function load(){ const r=await window.Api.apiRequest('/api/assets'); all=(r.data&&r.data.data)||[]; applyFilter(); }
document.getElementById('createAssetForm').onsubmit=async(e)=>{e.preventDefault(); const body={assetTag:assetTag.value,assetName:assetName.value,assetType:assetType.value,status:status.value}; await window.Api.apiRequest('/api/assets',{method:'POST',body:JSON.stringify(body)}); e.target.reset(); await load();};
load();})();
