(function(){ if(!window.App.ensureLoggedIn()) return; window.App.renderNav('company-transaction-destinations.html');
let editingId=null; let transactions=[];
const val=(id)=>document.getElementById(id);
function payload(){return {companyTransactionId:Number(val('destinationTransactionId').value||0),destinationCode:val('destinationCode').value.trim(),destinationName:val('destinationName').value.trim(),destinationSubtitle:val('destinationSubtitle').value.trim()||null,queueTypeId:val('destinationQueueTypeId').value?Number(val('destinationQueueTypeId').value):null,sortOrder:Number(val('destinationSortOrder').value||0),status:val('destinationStatus').value};}
function reset(){editingId=null;val('destinationFormTitle').textContent='Create Destination';val('destinationCode').value='';val('destinationName').value='';val('destinationSubtitle').value='';val('destinationQueueTypeId').value='';val('destinationSortOrder').value=0;val('destinationStatus').value='ACTIVE';}
async function loadTransactions(){
 const companies=(await window.Api.apiRequest('/companies/list')).data?.data||[]; let opts='';
 for(const c of companies){const tx=(await window.Api.apiRequest(`/company-transactions/company/${c.id}`)).data?.data||[]; tx.forEach(t=>{opts+=`<option value="${t.id}">${c.companyShortName} - ${t.transactionName}</option>`;transactions.push(t);});}
 val('destinationTransactionId').innerHTML=opts; val('filterTransactionId').innerHTML=opts;
}
async function loadList(){const tid=Number(val('filterTransactionId').value||0); if(!tid)return; const res=await window.Api.apiRequest(`/company-transaction-destinations/company-transaction/${tid}`); window.App.setDebug('debugPanel',res); const rows=res.data?.data||[];
 val('destinationsTableBody').innerHTML=rows.map(r=>`<tr><td>${r.destinationCode}</td><td>${r.destinationName}</td><td>${r.destinationSubtitle||''}</td><td>${r.queueTypeId||''}</td><td>${r.sortOrder}</td><td>${r.status}</td><td><button class="action-btn" data-e="${r.id}">Edit</button><button class="action-btn secondary" data-t="${r.id}" data-s="${r.status==='ACTIVE'?'INACTIVE':'ACTIVE'}">Toggle</button><button class="action-btn danger" data-d="${r.id}">Deactivate</button></td></tr>`).join('');
 document.querySelectorAll('[data-e]').forEach(b=>b.onclick=async()=>{const d=(await window.Api.apiRequest(`/company-transaction-destinations/${b.dataset.e}`)).data?.data; if(!d)return; editingId=Number(b.dataset.e);val('destinationFormTitle').textContent=`Edit Destination #${editingId}`;val('destinationTransactionId').value=String(d.companyTransactionId);val('destinationCode').value=d.destinationCode;val('destinationName').value=d.destinationName;val('destinationSubtitle').value=d.destinationSubtitle||'';val('destinationQueueTypeId').value=d.queueTypeId||'';val('destinationSortOrder').value=d.sortOrder;val('destinationStatus').value=d.status;});
 document.querySelectorAll('[data-t]').forEach(b=>b.onclick=async()=>{await window.Api.apiRequest(`/company-transaction-destinations/toggle/${b.dataset.t}`,{method:'PATCH',body:JSON.stringify({status:b.dataset.s})}); await loadList();});
 document.querySelectorAll('[data-d]').forEach(b=>b.onclick=async()=>{await window.Api.apiRequest(`/company-transaction-destinations/deactivate/${b.dataset.d}`,{method:'DELETE'}); await loadList();});
}
val('saveDestinationBtn').onclick=async()=>{const ep=editingId?`/company-transaction-destinations/update/${editingId}`:'/company-transaction-destinations/create'; const m=editingId?'PUT':'POST'; const res=await window.Api.apiRequest(ep,{method:m,body:JSON.stringify(payload())}); window.App.setDebug('debugPanel',res); if(res.ok){reset(); await loadList();}};
val('cancelDestinationEditBtn').onclick=reset; val('filterTransactionId').onchange=loadList;
(async()=>{await loadTransactions(); reset(); await loadList();})();
})();
