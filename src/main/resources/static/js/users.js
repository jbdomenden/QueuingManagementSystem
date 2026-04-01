(function(){ if(!window.App.ensureLoggedIn()) return; window.App.renderNav('users.html');
let all=[];
function render(rows){ document.getElementById('usersBody').innerHTML=rows.map(u=>`<tr><td>${u.fullName}</td><td>${u.email}</td><td>${u.departmentId||''}</td><td>${u.role}</td><td>${u.isActive?'ACTIVE':'INACTIVE'}</td><td><button data-id='${u.id}' class='secondary toggle'>${u.isActive?'Deactivate':'Activate'}</button></td></tr>`).join('');
 document.querySelectorAll('.toggle').forEach(b=>b.onclick=async()=>{const id=b.dataset.id;const user=all.find(x=>String(x.id)===id);await window.Api.apiRequest(`/api/users/${id}/status`,{method:'PATCH',body:JSON.stringify({isActive:!user.isActive})});await load();}); }
async function load(){ const r=await window.Api.apiRequest('/api/users'); all=(r.data&&r.data.data)||[]; applyFilter(); }
function applyFilter(){const q=(document.getElementById('userSearch').value||'').toLowerCase(); render(all.filter(u=>u.fullName.toLowerCase().includes(q)||u.email.toLowerCase().includes(q)));}
document.getElementById('userSearch').oninput=applyFilter;
document.getElementById('createUserForm').onsubmit=async(e)=>{e.preventDefault(); const body={email:email.value,password:password.value,fullName:fullName.value,role:role.value,isActive:true,forcePasswordChange:true}; await window.Api.apiRequest('/api/users',{method:'POST',body:JSON.stringify(body)}); e.target.reset(); await load();};
load();})();
