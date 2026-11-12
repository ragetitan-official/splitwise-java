// State management
let currentUserView = 'dashboard';
let users = [];
let groups = [];
let currentGroup = null;

// DOM Elements
const sidebar = document.getElementById('sidebar');
const hamburgerBtn = document.getElementById('hamburgerBtn');
const navLinks = document.querySelectorAll('.nav-link');
const views = document.querySelectorAll('.view');
const toastContainer = document.getElementById('toast-container');

// Dashboard Stats
const statGroups = document.getElementById('stat-groups');
const statUsers = document.getElementById('stat-users');
const statExpenses = document.getElementById('stat-expenses');
const recentActivity = document.getElementById('recent-activity');

// Users View
const usersList = document.getElementById('users-list');
const btnCreateUser = document.getElementById('btn-create-user');
const modalCreateUser = document.getElementById('modal-create-user');
const formCreateUser = document.getElementById('form-create-user');

// Groups View
const groupsListView = document.getElementById('groups-list-view');
const groupDetailView = document.getElementById('group-detail-view');
const groupsList = document.getElementById('groups-list');
const btnCreateGroup = document.getElementById('btn-create-group');
const modalCreateGroup = document.getElementById('modal-create-group');
const formCreateGroup = document.getElementById('form-create-group');
const groupMemberCheckboxes = document.getElementById('group-member-checkboxes');

// Group Detail View
const btnBackGroups = document.getElementById('btn-back-groups');
const groupDetailName = document.getElementById('group-detail-name');
const btnAddExpense = document.getElementById('btn-add-expense');
const btnDeleteGroup = document.getElementById('btn-delete-group');
const btnAddMember = document.getElementById('btn-add-member');
const groupMembersChips = document.getElementById('group-members');
const expensesTbody = document.getElementById('expenses-tbody');
const noExpenses = document.getElementById('no-expenses');
const settlementsTbody = document.getElementById('settlements-tbody');
const noSettlements = document.getElementById('no-settlements');

// Modals
const modalAddExpense = document.getElementById('modal-add-expense');
const formAddExpense = document.getElementById('form-add-expense');
const expensePaidBy = document.getElementById('expense-paidby');
const expenseSplitType = document.getElementById('expense-splittype');
const percentageInputs = document.getElementById('percentage-inputs');
const percentageFields = document.getElementById('percentage-fields');

const modalAddMember = document.getElementById('modal-add-member');
const formAddMember = document.getElementById('form-add-member');
const memberSelect = document.getElementById('member-select');

// Reports View
const reportGroupSelect = document.getElementById('report-group-select');
const btnGenerateReport = document.getElementById('btn-generate-report');
const reportOutput = document.getElementById('report-output');
const reportText = document.getElementById('report-text');

// API Base URL
const API_BASE = '/api';

// --- Toast Utility ---
function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerText = message;
  toastContainer.appendChild(toast);
  setTimeout(() => {
    toast.style.animation = 'fadeOut 0.3s forwards';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// --- Navigation ---
function switchView(viewName) {
  currentUserView = viewName;
  navLinks.forEach(link => {
    link.classList.toggle('active', link.dataset.view === viewName);
  });
  views.forEach(view => {
    view.classList.toggle('active', view.id === `view-${viewName}`);
  });

  if (viewName === 'dashboard') loadDashboard();
  if (viewName === 'users') loadUsers();
  if (viewName === 'groups') {
    showGroupsList();
    loadGroups();
  }
  if (viewName === 'reports') loadReportGroups();
}

navLinks.forEach(link => {
  link.addEventListener('click', (e) => {
    e.preventDefault();
    switchView(link.dataset.view);
  });
});

if (hamburgerBtn) {
  hamburgerBtn.addEventListener('click', () => {
    sidebar.classList.toggle('open');
  });
}

// Modal handling
document.querySelectorAll('[data-dismiss="modal"]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.modal-overlay').forEach(modal => modal.classList.remove('active'));
  });
});

function openModal(modal) {
  modal.classList.add('active');
}

function closeModal(modal) {
  modal.classList.remove('active');
}

// --- API Helpers ---
async function apiRequest(endpoint, options = {}) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || 'API request failed');
    }
    return await res.json();
  } catch (error) {
    showToast(error.message, 'error');
    throw error;
  }
}

// --- Dashboard ---
async function loadDashboard() {
  try {
    const [fetchedUsers, fetchedGroups] = await Promise.all([
      apiRequest('/users'),
      apiRequest('/groups')
    ]);
    users = fetchedUsers;
    groups = fetchedGroups;

    statUsers.innerText = users.length;
    statGroups.innerText = groups.length;

    let totalExpCount = 0;
    const activities = [];

    for (const g of groups) {
      if (g.expenses) {
        totalExpCount += g.expenses.length;
        g.expenses.forEach(exp => {
          activities.push({
            desc: `"${exp.description}" ($${Number(exp.amount).toFixed(2)}) in ${g.name}`,
            time: exp.createdAt || ''
          });
        });
      }
    }
    statExpenses.innerText = totalExpCount;

    recentActivity.innerHTML = '';
    if (activities.length === 0) {
      recentActivity.innerHTML = '<li class="empty-state">No recent activity yet.</li>';
    } else {
      activities.slice(-5).reverse().forEach(act => {
        const li = document.createElement('li');
        li.className = 'activity-item';
        li.innerHTML = `<span>💸 ${escapeHtml(act.desc)}</span><span class="activity-time">${act.time}</span>`;
        recentActivity.appendChild(li);
      });
    }
  } catch (err) {
    console.error(err);
  }
}

// --- Users ---
async function loadUsers() {
  try {
    users = await apiRequest('/users');
    usersList.innerHTML = '';
    if (users.length === 0) {
      usersList.innerHTML = '<div class="empty-state">No users yet. Create one to get started!</div>';
      return;
    }
    users.forEach(u => {
      const card = document.createElement('div');
      card.className = 'card user-card';
      card.innerHTML = `
        <div class="card-avatar">👤</div>
        <div class="card-title">${escapeHtml(u.name)}</div>
        <div class="card-subtitle">${escapeHtml(u.email)}</div>
      `;
      usersList.appendChild(card);
    });
  } catch (err) {
    console.error(err);
  }
}

btnCreateUser.addEventListener('click', () => {
  formCreateUser.reset();
  openModal(modalCreateUser);
});

formCreateUser.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('user-name').value.trim();
  const email = document.getElementById('user-email').value.trim();
  try {
    await apiRequest('/users', {
      method: 'POST',
      body: JSON.stringify({ name, email })
    });
    showToast(`User "${name}" created!`, 'success');
    closeModal(modalCreateUser);
    loadUsers();
  } catch (err) {
    console.error(err);
  }
});

// --- Groups ---
async function loadGroups() {
  try {
    groups = await apiRequest('/groups');
    groupsList.innerHTML = '';
    if (groups.length === 0) {
      groupsList.innerHTML = '<div class="empty-state">No groups yet. Create one to get started!</div>';
      return;
    }
    groups.forEach(g => {
      const card = document.createElement('div');
      card.className = 'card group-card';
      const memberCount = g.members ? g.members.length : 0;
      const expenseCount = g.expenses ? g.expenses.length : 0;
      card.innerHTML = `
        <div class="card-header">
          <div class="card-title">📁 ${escapeHtml(g.name)}</div>
        </div>
        <div class="card-body">
          <p>👥 ${memberCount} Members &nbsp;|&nbsp; 💸 ${expenseCount} Expenses</p>
        </div>
        <button class="btn btn-secondary btn-block">View Details →</button>
      `;
      card.addEventListener('click', () => openGroupDetail(g.id));
      groupsList.appendChild(card);
    });
  } catch (err) {
    console.error(err);
  }
}

btnCreateGroup.addEventListener('click', async () => {
  formCreateGroup.reset();
  users = await apiRequest('/users');
  groupMemberCheckboxes.innerHTML = '';
  if (users.length === 0) {
    groupMemberCheckboxes.innerHTML = '<p class="text-muted">No users found. Create users first.</p>';
  } else {
    users.forEach(u => {
      const label = document.createElement('label');
      label.className = 'checkbox-label';
      label.innerHTML = `
        <input type="checkbox" value="${u.id}">
        <span>${escapeHtml(u.name)} (${escapeHtml(u.email)})</span>
      `;
      groupMemberCheckboxes.appendChild(label);
    });
  }
  openModal(modalCreateGroup);
});

formCreateGroup.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('group-name').value.trim();
  const selectedMemberIds = Array.from(
    groupMemberCheckboxes.querySelectorAll('input[type="checkbox"]:checked')
  ).map(cb => cb.value);

  try {
    await apiRequest('/groups', {
      method: 'POST',
      body: JSON.stringify({ name, memberIds: selectedMemberIds })
    });
    showToast(`Group "${name}" created!`, 'success');
    closeModal(modalCreateGroup);
    loadGroups();
  } catch (err) {
    console.error(err);
  }
});

function showGroupsList() {
  groupsListView.classList.remove('hidden');
  groupDetailView.classList.add('hidden');
}

btnBackGroups.addEventListener('click', showGroupsList);

async function openGroupDetail(groupId) {
  try {
    currentGroup = await apiRequest(`/groups/${groupId}`);
    groupsListView.classList.add('hidden');
    groupDetailView.classList.remove('hidden');
    renderGroupDetail();
  } catch (err) {
    console.error(err);
  }
}

async function renderGroupDetail() {
  if (!currentGroup) return;
  groupDetailName.innerText = currentGroup.name;

  // Render members
  groupMembersChips.innerHTML = '';
  if (currentGroup.members && currentGroup.members.length > 0) {
    currentGroup.members.forEach(m => {
      const chip = document.createElement('span');
      chip.className = 'member-chip';
      chip.innerText = `👤 ${m.name}`;
      groupMembersChips.appendChild(chip);
    });
  } else {
    groupMembersChips.innerHTML = '<span class="text-muted">No members yet.</span>';
  }

  // Render expenses
  expensesTbody.innerHTML = '';
  if (currentGroup.expenses && currentGroup.expenses.length > 0) {
    noExpenses.classList.add('hidden');
    currentGroup.expenses.forEach(exp => {
      const tr = document.createElement('tr');
      const paidByName = exp.paidBy ? exp.paidBy.name : 'Unknown';
      tr.innerHTML = `
        <td><strong>${escapeHtml(exp.description)}</strong></td>
        <td>$${Number(exp.amount).toFixed(2)}</td>
        <td>${escapeHtml(paidByName)}</td>
        <td><span class="badge">${exp.splitType}</span></td>
        <td>${exp.createdAt || 'N/A'}</td>
      `;
      expensesTbody.appendChild(tr);
    });
  } else {
    noExpenses.classList.remove('hidden');
  }

  // Render settlements
  loadSettlements(currentGroup.id);
}

async function loadSettlements(groupId) {
  try {
    const settlements = await apiRequest(`/groups/${groupId}/settlements`);
    settlementsTbody.innerHTML = '';
    if (settlements && settlements.length > 0) {
      noSettlements.classList.add('hidden');
      settlements.forEach(s => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="text-danger"><strong>${escapeHtml(s.fromUser ? s.fromUser.name : 'User')}</strong></td>
          <td class="text-success"><strong>${escapeHtml(s.toUser ? s.toUser.name : 'User')}</strong></td>
          <td><span class="settlement-amount">$${Number(s.amount).toFixed(2)}</span></td>
        `;
        settlementsTbody.appendChild(tr);
      });
    } else {
      noSettlements.classList.remove('hidden');
    }
  } catch (err) {
    console.error(err);
  }
}

btnDeleteGroup.addEventListener('click', async () => {
  if (!currentGroup) return;
  if (confirm(`Are you sure you want to delete group "${currentGroup.name}"?`)) {
    try {
      await apiRequest(`/groups/${currentGroup.id}`, { method: 'DELETE' });
      showToast('Group deleted.', 'info');
      showGroupsList();
      loadGroups();
    } catch (err) {
      console.error(err);
    }
  }
});

// --- Add Member to Group ---
btnAddMember.addEventListener('click', async () => {
  if (!currentGroup) return;
  users = await apiRequest('/users');
  const existingMemberIds = (currentGroup.members || []).map(m => m.id);
  const availableUsers = users.filter(u => !existingMemberIds.includes(u.id));

  memberSelect.innerHTML = '<option value="">-- Select a user --</option>';
  if (availableUsers.length === 0) {
    memberSelect.innerHTML = '<option value="">No other users available</option>';
  } else {
    availableUsers.forEach(u => {
      const opt = document.createElement('option');
      opt.value = u.id;
      opt.innerText = `${u.name} (${u.email})`;
      memberSelect.appendChild(opt);
    });
  }
  openModal(modalAddMember);
});

formAddMember.addEventListener('submit', async (e) => {
  e.preventDefault();
  const userId = memberSelect.value;
  if (!userId) return;
  try {
    await apiRequest(`/groups/${currentGroup.id}/members`, {
      method: 'POST',
      body: JSON.stringify({ userId })
    });
    showToast('Member added!', 'success');
    closeModal(modalAddMember);
    openGroupDetail(currentGroup.id);
  } catch (err) {
    console.error(err);
  }
});

// --- Add Expense ---
btnAddExpense.addEventListener('click', () => {
  if (!currentGroup || !currentGroup.members || currentGroup.members.length === 0) {
    showToast('Please add members to the group before adding expenses.', 'warning');
    return;
  }
  formAddExpense.reset();
  expensePaidBy.innerHTML = '<option value="">-- Select who paid --</option>';
  currentGroup.members.forEach(m => {
    const opt = document.createElement('option');
    opt.value = m.id;
    opt.innerText = m.name;
    expensePaidBy.appendChild(opt);
  });

  percentageInputs.classList.add('hidden');
  openModal(modalAddExpense);
});

expenseSplitType.addEventListener('change', () => {
  if (expenseSplitType.value === 'PERCENTAGE') {
    percentageInputs.classList.remove('hidden');
    percentageFields.innerHTML = '';
    currentGroup.members.forEach(m => {
      const div = document.createElement('div');
      div.className = 'form-group percentage-row';
      div.innerHTML = `
        <label>${escapeHtml(m.name)} (%)</label>
        <input type="number" step="0.01" min="0" max="100" class="form-input percentage-input" data-user-id="${m.id}" placeholder="0.00" required>
      `;
      percentageFields.appendChild(div);
    });
  } else {
    percentageInputs.classList.add('hidden');
  }
});

formAddExpense.addEventListener('submit', async (e) => {
  e.preventDefault();
  const description = document.getElementById('expense-description').value.trim();
  const amount = parseFloat(document.getElementById('expense-amount').value);
  const paidById = expensePaidBy.value;
  const splitType = expenseSplitType.value;

  const payload = {
    description,
    amount: amount.toString(),
    paidById,
    splitType
  };

  if (splitType === 'PERCENTAGE') {
    const percentages = {};
    let sum = 0;
    const inputs = percentageFields.querySelectorAll('.percentage-input');
    inputs.forEach(input => {
      const val = parseFloat(input.value) || 0;
      percentages[input.dataset.userId] = val.toString();
      sum += val;
    });

    if (Math.abs(sum - 100) > 0.01) {
      showToast(`Percentages must sum to 100% (currently ${sum.toFixed(2)}%)`, 'error');
      return;
    }
    payload.percentages = percentages;
  }

  try {
    await apiRequest(`/groups/${currentGroup.id}/expenses`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    showToast('Expense added successfully!', 'success');
    closeModal(modalAddExpense);
    openGroupDetail(currentGroup.id);
  } catch (err) {
    console.error(err);
  }
});

// --- Reports ---
async function loadReportGroups() {
  try {
    groups = await apiRequest('/groups');
    reportGroupSelect.innerHTML = '<option value="">-- Select a group --</option>';
    groups.forEach(g => {
      const opt = document.createElement('option');
      opt.value = g.id;
      opt.innerText = g.name;
      reportGroupSelect.appendChild(opt);
    });
  } catch (err) {
    console.error(err);
  }
}

btnGenerateReport.addEventListener('click', async () => {
  const groupId = reportGroupSelect.value;
  if (!groupId) {
    showToast('Please select a group first.', 'warning');
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/groups/${groupId}/report`);
    if (!res.ok) throw new Error('Failed to generate report');
    const text = await res.text();
    reportText.innerText = text;
    reportOutput.classList.remove('hidden');
  } catch (err) {
    showToast(err.message, 'error');
  }
});

// Helper
function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, 
    tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
  );
}

// Initial Load
document.addEventListener('DOMContentLoaded', () => {
  loadDashboard();
});
