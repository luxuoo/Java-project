/* ============================================================
 * 健身房管理系统 - 单文件 Vue 3 + Element Plus 应用
 * ----------------------------------------------------------
 * 角色：
 *   ADMIN  完整后台 + 看板
 *   COACH  我的课程 / 排班 / 学员预约 / 通知 / 个人资料
 *   MEMBER 课程浏览 / 我的会员卡 / 我的预约 / 我的签到 / 我的消费 / 通知 / 个人资料
 * ============================================================ */
const { createApp, ref, reactive, computed, onMounted, watch, nextTick, h } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

// ====== HTTP 客户端 ======
const api = axios.create({ baseURL: '/api' });
api.interceptors.request.use(cfg => {
    const token = localStorage.getItem('token');
    if (token) cfg.headers.Authorization = 'Bearer ' + token;
    return cfg;
});
api.interceptors.response.use(r => {
    const data = r.data;
    // 兼容后端"业务失败但 HTTP 200"的回包：把 Result.fail(...) 转成异常，
    // 让调用方的 try/catch 能捕到，避免页面表现为"点了没反应"。
    if (data && typeof data === 'object' && 'code' in data && data.code !== 200) {
        // 401/403 走拦截器后续分支；这里只处理业务码
        ElMessage.error(data.message || '操作失败');
        const err = new Error(data.message || '操作失败');
        err.bizCode = data.code;
        err.response = { status: 200, data };
        return Promise.reject(err);
    }
    return data;
}, err => {
    const status = err.response?.status;
    if (status === 401) {
        localStorage.clear();
        location.reload();
    } else if (status === 403) {
        ElMessage.error(err.response?.data?.message || '权限不足');
    } else {
        ElMessage.error(err.response?.data?.message || '请求失败');
    }
    return Promise.reject(err);
});

// ====== 图标 ======
const ICONS = {
    logo: '/icons/fitness.svg',
    member: '/icons/person.svg',
    equipment: '/icons/treadmill.svg',
    payment: '/icons/wallet.svg',
    notification: '/icons/bell.svg',
    course: '/icons/calendar.svg',
    checkin: '/icons/treadmill.svg',
    coach: '/icons/person.svg',
    dashboard: '/icons/fitness.svg',
    user: '/icons/person.svg',
};

// ====== 工具函数 ======
const fmtMoney = v => v == null || v === '' ? '0' : parseFloat(v).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
const safeUser = () => { try { return JSON.parse(localStorage.getItem('user') || '{}'); } catch (e) { return {}; } };
const roleLabel = r => ({ ADMIN: '管理员', COACH: '教练', MEMBER: '学员' })[r] || r;
const reservationStatusText = s => ({ 0: '已取消', 1: '已预约', 2: '已签到', 3: '已完成' })[s] || '未知';
const reservationTagType = s => ({ 0: 'info', 1: '', 2: 'success', 3: 'success' })[s] || '';
const cardStatusText = s => ['未激活', '使用中', '已过期', '已冻结'][s] || '未知';
const cardTier = name => {
    if (!name) return '';
    if (name.includes('至尊') || name.includes('白金')) return 'tier-platinum';
    if (name.includes('黄金') || name.includes('金')) return 'tier-gold';
    if (name.includes('银')) return 'tier-silver';
    return '';
};

// ============================================================
// 登录页
// ============================================================
const LoginTemplate = {
    template: `
    <div class="login-container">
        <div class="login-theme-toggle" @click="toggleTheme" :title="theme==='dark'?'切换到浅色':'切换到深色'">
            <svg v-if="theme==='dark'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </div>
        <div class="login-card">
            <el-card>
                <div class="login-title">
                    <img src="/icons/fitness.svg" class="logo-img" />
                    <h2>健身房管理系统</h2>
                    <p>Gym Management System</p>
                </div>
                <el-form :model="form" @submit.prevent="login">
                    <el-form-item><el-input v-model="form.username" placeholder="请输入用户名" size="large" /></el-form-item>
                    <el-form-item><el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password @keyup.enter="login" /></el-form-item>
                    <el-form-item><el-button type="primary" :loading="loading" @click="login" size="large" style="width:100%">登 录</el-button></el-form-item>
                </el-form>
                <div class="login-accounts">
                    <span @click="fillDemo('admin', 'admin123')" style="cursor:pointer">管理员: admin / admin123</span>
                    <span @click="fillDemo('coach01', 'coach123')" style="cursor:pointer">教练: coach01 / coach123</span>
                    <span @click="fillDemo('member01', 'member123')" style="cursor:pointer">学员: member01 / member123</span>
                </div>
            </el-card>
        </div>
    </div>`,
    setup() {
        const form = reactive({ username: '', password: '' });
        const loading = ref(false);
        const fillDemo = (u, p) => { form.username = u; form.password = p; };
        const login = async () => {
            if (!form.username || !form.password) { ElMessage.warning('请填写用户名和密码'); return; }
            loading.value = true;
            try {
                const res = await api.post('/auth/login', form);
                if (res.code === 200) {
                    localStorage.setItem('token', res.data.token);
                    localStorage.setItem('user', JSON.stringify(res.data));
                    ElMessage.success('登录成功，欢迎 ' + (res.data.realName || res.data.username));
                    location.reload();
                } else { ElMessage.error(res.message); }
            } finally { loading.value = false; }
        };

        // 登录页主题切换（与主框架共享同一份 localStorage 配置）
        const theme = ref(localStorage.getItem('theme') || 'light');
        const toggleTheme = () => {
            theme.value = theme.value === 'dark' ? 'light' : 'dark';
            localStorage.setItem('theme', theme.value);
            document.documentElement.setAttribute('data-theme', theme.value);
            if (theme.value === 'dark') document.documentElement.classList.add('dark');
            else document.documentElement.classList.remove('dark');
        };

        return { form, loading, login, fillDemo, theme, toggleTheme };
    }
};


// ============================================================
// 数据看板（ADMIN / COACH）
// ============================================================
const DashboardTemplate = {
    template: `
    <div>
        <div class="welcome-banner">
            <div>
                <h2>欢迎回来，{{userName}}</h2>
                <p>今天是 {{today}}，{{greeting}}</p>
            </div>
            <img src="/icons/fitness.svg" class="banner-img" />
        </div>
        <el-row :gutter="16" class="stat-row">
            <el-col :xs="12" :sm="6"><div class="stat-card stat-blue"><div class="icon-box"><img src="/icons/person.svg" /></div><div class="stat-info"><h3>{{stats.totalMembers}}</h3><p>总会员数</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-green"><div class="icon-box"><img src="/icons/person.svg" /></div><div class="stat-info"><h3>{{stats.activeMembers}}</h3><p>活跃会员</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-orange"><div class="icon-box"><img src="/icons/person.svg" /></div><div class="stat-info"><h3>{{stats.totalCoaches}}</h3><p>教练数量</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-red"><div class="icon-box"><img src="/icons/treadmill.svg" /></div><div class="stat-info"><h3>{{stats.todayCheckIns}}</h3><p>今日签到</p></div></div></el-col>
        </el-row>
        <el-row :gutter="16" class="stat-row">
            <el-col :xs="12" :sm="6"><div class="stat-card stat-blue"><div class="icon-box"><img src="/icons/calendar.svg" /></div><div class="stat-info"><h3>{{stats.totalCourses}}</h3><p>总课程数</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-green"><div class="icon-box"><img src="/icons/calendar.svg" /></div><div class="stat-info"><h3>{{stats.upcomingCourses}}</h3><p>即将开课</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-orange"><div class="icon-box"><img src="/icons/wallet.svg" /></div><div class="stat-info"><h3>¥{{fmtMoney(stats.monthRevenue)}}</h3><p>本月营收</p></div></div></el-col>
            <el-col :xs="12" :sm="6"><div class="stat-card stat-red"><div class="icon-box"><img src="/icons/wallet.svg" /></div><div class="stat-info"><h3>¥{{fmtMoney(stats.totalRevenue)}}</h3><p>累计营收</p></div></div></el-col>
        </el-row>
        <el-row :gutter="16">
            <el-col :span="16"><div class="chart-card"><el-card header="月度营收趋势"><div ref="revenueChartRef" style="height:320px"></div></el-card></div></el-col>
            <el-col :span="8"><div class="chart-card"><el-card header="会员性别分布"><div ref="genderChartRef" style="height:320px"></div></el-card></div></el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top:16px">
            <el-col :span="12"><div class="chart-card"><el-card header="课程分类统计"><div ref="categoryChartRef" style="height:300px"></div></el-card></div></el-col>
            <el-col :span="12"><div class="chart-card"><el-card header="近7天签到趋势"><div ref="checkinChartRef" style="height:300px"></div></el-card></div></el-col>
        </el-row>
        <div class="quick-actions">
            <div class="quick-action-item" v-for="a in quickActions" :key="a.label" @click="navigate(a.key)">
                <img :src="a.icon" />
                <span>{{a.label}}</span>
            </div>
        </div>
    </div>`,
    setup() {
        const stats = reactive({});
        const userName = ref(safeUser().realName || safeUser().username || '管理员');
        const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' });
        const greeting = computed(() => { const h = new Date().getHours(); if (h < 12) return '上午好！'; if (h < 18) return '下午好！'; return '晚上好！'; });

        const revenueChartRef = ref(null);
        const genderChartRef = ref(null);
        const categoryChartRef = ref(null);
        const checkinChartRef = ref(null);

        const role = safeUser().role;
        const quickActions = computed(() => {
            if (role === 'ADMIN') return [
                { key: 'member', label: '会员管理', icon: '/icons/person.svg' },
                { key: 'coach', label: '教练管理', icon: '/icons/person.svg' },
                { key: 'course', label: '课程管理', icon: '/icons/calendar.svg' },
                { key: 'payment', label: '财务管理', icon: '/icons/wallet.svg' },
            ];
            return [
                { key: 'my-courses', label: '我的课程', icon: '/icons/calendar.svg' },
                { key: 'member', label: '会员档案', icon: '/icons/person.svg' },
                { key: 'checkin', label: '签到管理', icon: '/icons/treadmill.svg' },
                { key: 'profile', label: '个人资料', icon: '/icons/person.svg' },
            ];
        });

        const initChart = (el, option) => {
            if (!el) return null;
            const chart = echarts.init(el);
            chart.setOption(option);
            return chart;
        };

        const load = async () => {
            try {
                const res = await api.get('/dashboard/stats');
                if (res.code === 200) {
                    Object.assign(stats, res.data);
                    await nextTick();
                    // 月度营收
                    const revData = res.data.revenueByMonth || [];
                    initChart(revenueChartRef.value, {
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', data: revData.map(d => d.month || d.name) },
                        yAxis: { type: 'value' },
                        series: [{ type: 'line', data: revData.map(d => d.value || d.amount), smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: '#409eff' } }],
                        grid: { left: 60, right: 20, top: 20, bottom: 30 }
                    });
                    // 性别分布
                    const genderData = res.data.memberByGender || [];
                    initChart(genderChartRef.value, {
                        tooltip: { trigger: 'item' },
                        series: [{ type: 'pie', radius: ['40%', '70%'], data: genderData.map(d => ({ name: d.name || d.gender, value: d.value || d.count })), label: { show: true, formatter: '{b}: {c}人' } }]
                    });
                    // 课程分类
                    const catData = res.data.courseByCategory || [];
                    initChart(categoryChartRef.value, {
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', data: catData.map(d => d.name || d.category) },
                        yAxis: { type: 'value' },
                        series: [{ type: 'bar', data: catData.map(d => d.value || d.count), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#67c23a' } }],
                        grid: { left: 60, right: 20, top: 20, bottom: 30 }
                    });
                    // 签到趋势
                    const ciData = res.data.checkInByDay || [];
                    initChart(checkinChartRef.value, {
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', data: ciData.map(d => d.date || d.name) },
                        yAxis: { type: 'value' },
                        series: [{ type: 'bar', data: ciData.map(d => d.value || d.count), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#e6a23c' } }],
                        grid: { left: 60, right: 20, top: 20, bottom: 30 }
                    });
                }
            } catch (e) { console.error('Dashboard load error', e); }
        };

        const navigate = Vue.inject('navigate', () => {});

        onMounted(load);
        return { stats, userName, today, greeting, revenueChartRef, genderChartRef, categoryChartRef, checkinChartRef, quickActions, fmtMoney, navigate };
    }
};

// ============================================================
// 会员管理（ADMIN 写；ADMIN/COACH 读）
// ============================================================
const MemberTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/person.svg" />会员管理</h2><el-button v-if="isAdmin" type="primary" @click="showForm()">新增会员</el-button></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="搜索姓名/手机/编号" clearable style="width:250px" @keyup.enter="load" />
            <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="load">
                <el-option label="正常" :value="1" /><el-option label="冻结" :value="2" /><el-option label="注销" :value="0" />
            </el-select>
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="memberNo" label="会员编号" width="120" />
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="gender" label="性别" width="70"><template #default="{row}">{{row.gender===1?'男':'女'}}</template></el-table-column>
            <el-table-column prop="phone" label="手机" width="130" />
            <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':row.status===2?'warning':'info'" size="small">{{['注销','正常','冻结'][row.status]}}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="170" />
            <el-table-column label="操作" width="280">
                <template #default="{row}">
                    <el-button size="small" @click="showDetail(row)">详情</el-button>
                    <el-button v-if="isAdmin" size="small" @click="showForm(row)">编辑</el-button>
                    <el-button v-if="isAdmin" size="small" type="danger" @click="del(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" :title="form.id?'编辑会员':'新增会员'" width="650px">
            <el-form :model="form" label-width="100px">
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col><el-col :span="12"><el-form-item label="性别"><el-select v-model="form.gender" style="width:100%"><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item></el-col><el-col :span="12"><el-form-item label="身份证"><el-input v-model="form.idCard" /></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="生日"><el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="正常" :value="1" /><el-option label="冻结" :value="2" /><el-option label="注销" :value="0" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" /></el-form-item></el-col><el-col :span="12"><el-form-item label="紧急电话"><el-input v-model="form.emergencyPhone" /></el-form-item></el-col></el-row>
                <el-form-item label="健康备注"><el-input v-model="form.healthNote" type="textarea" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
        </el-dialog>
        <el-drawer v-model="detailVisible" :title="'会员详情 - '+(detailMember?.name||'')" size="55%">
            <template v-if="detailMember">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="会员编号">{{detailMember.memberNo}}</el-descriptions-item>
                    <el-descriptions-item label="姓名">{{detailMember.name}}</el-descriptions-item>
                    <el-descriptions-item label="性别">{{detailMember.gender===1?'男':'女'}}</el-descriptions-item>
                    <el-descriptions-item label="手机">{{detailMember.phone}}</el-descriptions-item>
                    <el-descriptions-item label="身份证">{{detailMember.idCard||'-'}}</el-descriptions-item>
                    <el-descriptions-item label="生日">{{detailMember.birthday||'-'}}</el-descriptions-item>
                    <el-descriptions-item label="紧急联系人">{{detailMember.emergencyContact||'-'}}</el-descriptions-item>
                    <el-descriptions-item label="紧急电话">{{detailMember.emergencyPhone||'-'}}</el-descriptions-item>
                    <el-descriptions-item label="健康备注" :span="2">{{detailMember.healthNote||'-'}}</el-descriptions-item>
                </el-descriptions>
                <el-tabs v-model="detailTab" style="margin-top:16px">
                    <el-tab-pane label="会员卡" name="cards">
                        <div v-if="detailCards.length" class="card-list">
                            <div v-for="c in detailCards" :key="c.id" :class="['member-card', cardTier(c.cardName)]">
                                <div class="status-tag">{{cardStatusText(c.status)}}</div>
                                <div class="card-name">{{c.cardName||c.cardType}}</div>
                                <div class="card-type">{{c.cardType}}</div>
                                <div class="price">¥{{c.price}}</div>
                                <div class="card-row"><span>到期</span><span>{{c.endDate||'-'}}</span></div>
                            </div>
                        </div>
                        <el-empty v-else description="暂无会员卡" />
                    </el-tab-pane>
                    <el-tab-pane label="预约记录" name="reservations">
                        <el-table :data="detailReservations" size="small">
                            <el-table-column label="课程"><template #default="{row}">课程#{{row.courseId}}</template></el-table-column>
                            <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="reservationTagType(row.status)" size="small">{{reservationStatusText(row.status)}}</el-tag></template></el-table-column>
                            <el-table-column prop="reserveTime" label="预约时间" width="170" />
                        </el-table>
                    </el-tab-pane>
                    <el-tab-pane label="签到记录" name="checkins">
                        <el-table :data="detailCheckins" size="small">
                            <el-table-column prop="checkInTime" label="进场时间" />
                            <el-table-column prop="checkOutTime" label="离场时间"><template #default="{row}">{{row.checkOutTime||'在场中'}}</template></el-table-column>
                            <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
                        </el-table>
                    </el-tab-pane>
                    <el-tab-pane label="消费记录" name="payments">
                        <el-table :data="detailPayments" size="small">
                            <el-table-column prop="type" label="类型" width="80" />
                            <el-table-column prop="amount" label="金额" width="120"><template #default="{row}"><span :class="row.amount>=0?'amount-positive':'amount-negative'">{{row.amount>=0?'+':''}}¥{{row.amount}}</span></template></el-table-column>
                            <el-table-column prop="payMethod" label="支付方式" width="100" />
                            <el-table-column prop="createdAt" label="时间" width="170" />
                        </el-table>
                    </el-tab-pane>
                </el-tabs>
            </template>
        </el-drawer>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, keyword: '', status: null });
        const dlgVisible = ref(false); const form = reactive({});
        const isAdmin = computed(() => safeUser().role === 'ADMIN');

        const detailVisible = ref(false); const detailMember = ref(null);
        const detailTab = ref('cards');
        const detailCards = ref([]); const detailReservations = ref([]); const detailCheckins = ref([]); const detailPayments = ref([]);

        const load = async () => {
            loading.value = true;
            try { const res = await api.get('/member/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } }
            finally { loading.value = false; }
        };
        const showForm = row => {
            Object.assign(form, row ? { ...row } : { name: '', gender: 1, phone: '', idCard: '', birthday: '', emergencyContact: '', emergencyPhone: '', healthNote: '', status: 1 });
            dlgVisible.value = true;
        };
        const save = async () => {
            const res = form.id ? await api.put('/member/' + form.id, form) : await api.post('/member', form);
            if (res.code === 200) { ElMessage.success(res.message); dlgVisible.value = false; load(); }
        };
        const del = row => ElMessageBox.confirm('确定删除该会员？', '提示', { type: 'warning' }).then(async () => {
            const res = await api.delete('/member/' + row.id);
            if (res.code === 200) { ElMessage.success('删除成功'); load(); }
        }).catch(() => {});

        const showDetail = async row => {
            detailMember.value = row; detailTab.value = 'cards'; detailVisible.value = true;
            const [cRes, rRes, ciRes, pRes] = await Promise.all([
                api.get('/member/' + row.id + '/cards').catch(() => ({ code: 0, data: [] })),
                api.get('/member/' + row.id + '/reservations').catch(() => ({ code: 0, data: [] })),
                api.get('/member/' + row.id + '/checkins').catch(() => ({ code: 0, data: [] })),
                api.get('/member/' + row.id + '/payments').catch(() => ({ code: 0, data: [] })),
            ]);
            if (cRes.code === 200) detailCards.value = cRes.data;
            if (rRes.code === 200) detailReservations.value = rRes.data;
            if (ciRes.code === 200) detailCheckins.value = ciRes.data;
            if (pRes.code === 200) detailPayments.value = pRes.data;
        };

        onMounted(load);
        return { list, total, loading, query, dlgVisible, form, isAdmin, load, showForm, save, del, detailVisible, detailMember, detailTab, detailCards, detailReservations, detailCheckins, detailPayments, showDetail, cardStatusText, cardTier, reservationStatusText, reservationTagType };
    }
};

// ============================================================
// 教练管理（ADMIN 写；ADMIN/COACH 看列表）
// ============================================================
const CoachTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/person.svg" />教练管理</h2><el-button v-if="isAdmin" type="primary" @click="showForm()">新增教练</el-button></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="搜索姓名/手机/特长" clearable style="width:250px" @keyup.enter="load" />
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="coachNo" label="教练编号" width="130" />
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="gender" label="性别" width="70"><template #default="{row}">{{row.gender===1?'男':'女'}}</template></el-table-column>
            <el-table-column prop="specialty" label="擅长" />
            <el-table-column prop="level" label="等级" width="80"><template #default="{row}"><el-tag size="small">{{row.level}}</el-tag></template></el-table-column>
            <el-table-column prop="hourlyRate" label="课时费" width="100"><template #default="{row}">¥{{row.hourlyRate}}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{row.status===1?'在职':'离职'}}</el-tag></template></el-table-column>
            <el-table-column v-if="isAdmin" label="操作" width="200">
                <template #default="{row}"><el-button size="small" @click="showForm(row)">编辑</el-button><el-button size="small" type="danger" @click="del(row)">删除</el-button></template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" :title="form.id?'编辑教练':'新增教练'" width="600px">
            <el-form :model="form" label-width="80px">
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col><el-col :span="12"><el-form-item label="性别"><el-select v-model="form.gender" style="width:100%"><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item></el-col><el-col :span="12"><el-form-item label="等级"><el-select v-model="form.level" style="width:100%"><el-option v-for="l in ['初级','中级','高级','明星']" :key="l" :label="l" :value="l" /></el-select></el-form-item></el-col></el-row>
                <el-form-item label="擅长"><el-input v-model="form.specialty" placeholder="多个用逗号分隔" /></el-form-item>
                <el-form-item label="资质"><el-input v-model="form.certification" /></el-form-item>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="课时费"><el-input-number v-model="form.hourlyRate" :min="0" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="在职" :value="1" /><el-option label="离职" :value="0" /></el-select></el-form-item></el-col></el-row>
                <el-form-item label="简介"><el-input v-model="form.bio" type="textarea" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, keyword: '' });
        const dlgVisible = ref(false); const form = reactive({});
        const isAdmin = computed(() => safeUser().role === 'ADMIN');
        const load = async () => {
            loading.value = true;
            try { const res = await api.get('/coach/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } }
            finally { loading.value = false; }
        };
        const showForm = row => { Object.assign(form, row ? { ...row } : { name: '', gender: 1, phone: '', level: '中级', specialty: '', certification: '', hourlyRate: 200, status: 1, bio: '' }); dlgVisible.value = true; };
        const save = async () => { const res = form.id ? await api.put('/coach/' + form.id, form) : await api.post('/coach', form); if (res.code === 200) { ElMessage.success(res.message); dlgVisible.value = false; load(); } };
        const del = row => ElMessageBox.confirm('确定删除该教练？', '提示', { type: 'warning' }).then(async () => { const res = await api.delete('/coach/' + row.id); if (res.code === 200) { ElMessage.success('删除成功'); load(); } }).catch(() => {});
        onMounted(load);
        return { list, total, loading, query, dlgVisible, form, isAdmin, load, showForm, save, del };
    }
};

// ============================================================
// 课程管理（ADMIN/COACH 写；所有角色可读）
// ============================================================
const CourseTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/calendar.svg" />课程管理</h2><el-button v-if="canWrite" type="primary" @click="showForm()">新增课程</el-button></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width:200px" @keyup.enter="load" />
            <el-select v-model="query.type" placeholder="类型" clearable style="width:120px" @change="load"><el-option v-for="t in ['团课','私教','小班']" :key="t" :label="t" :value="t" /></el-select>
            <el-select v-model="query.category" placeholder="分类" clearable style="width:120px" @change="load"><el-option v-for="c in ['有氧','力量','柔韧','格斗','舞蹈']" :key="c" :label="c" :value="c" /></el-select>
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="name" label="课程名称" />
            <el-table-column prop="type" label="类型" width="80"><template #default="{row}"><el-tag size="small">{{row.type}}</el-tag></template></el-table-column>
            <el-table-column prop="category" label="分类" width="80" />
            <el-table-column prop="room" label="教室" width="120" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column label="容量" width="100"><template #default="{row}">{{row.currentCount}}/{{row.maxCapacity}}</template></el-table-column>
            <el-table-column prop="price" label="价格" width="80"><template #default="{row}">{{row.price>0?'¥'+row.price:'免费'}}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="['info','','success',''][row.status]||'info'" size="small">{{['已取消','报名中','进行中','已结束'][row.status]}}</el-tag></template></el-table-column>
            <el-table-column v-if="canWrite" label="操作" width="200">
                <template #default="{row}"><el-button size="small" @click="showForm(row)">编辑</el-button><el-button v-if="isAdmin" size="small" type="danger" @click="del(row)">删除</el-button></template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" :title="form.id?'编辑课程':'新增课程'" width="650px">
            <el-form :model="form" label-width="80px">
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item></el-col><el-col :span="6"><el-form-item label="类型"><el-select v-model="form.type" style="width:100%"><el-option v-for="t in ['团课','私教','小班']" :key="t" :label="t" :value="t" /></el-select></el-form-item></el-col><el-col :span="6"><el-form-item label="分类"><el-select v-model="form.category" style="width:100%"><el-option v-for="c in ['有氧','力量','柔韧','格斗','舞蹈']" :key="c" :label="c" :value="c" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="8"><el-form-item label="教室"><el-input v-model="form.room" /></el-form-item></el-col><el-col :span="8"><el-form-item label="最大人数"><el-input-number v-model="form.maxCapacity" :min="1" style="width:100%" /></el-form-item></el-col><el-col :span="8"><el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="教练"><el-select v-model="form.coachId" filterable style="width:100%" placeholder="选择教练"><el-option v-for="c in coachList" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="报名中" :value="1" /><el-option label="进行中" :value="2" /><el-option label="已结束" :value="3" /><el-option label="已取消" :value="0" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col></el-row>
                <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, keyword: '', type: '', category: '' });
        const dlgVisible = ref(false); const form = reactive({});
        const coachList = ref([]);
        const role = safeUser().role;
        const canWrite = computed(() => role === 'ADMIN' || role === 'COACH');
        const isAdmin = computed(() => role === 'ADMIN');
        const load = async () => {
            loading.value = true;
            try { const res = await api.get('/course/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } }
            finally { loading.value = false; }
        };
        const loadCoaches = async () => { const res = await api.get('/coach/list', { params: { page: 1, size: 100 } }); if (res.code === 200) coachList.value = res.data.records; };
        const showForm = row => { Object.assign(form, row ? { ...row } : { name: '', type: '团课', category: '有氧', room: '', maxCapacity: 20, price: 0, coachId: null, startTime: '', endTime: '', description: '', status: 1 }); dlgVisible.value = true; };
        const save = async () => { const res = form.id ? await api.put('/course/' + form.id, form) : await api.post('/course', form); if (res.code === 200) { ElMessage.success(res.message); dlgVisible.value = false; load(); } };
        const del = row => ElMessageBox.confirm('确定删除该课程？', '提示', { type: 'warning' }).then(async () => { const res = await api.delete('/course/' + row.id); if (res.code === 200) { ElMessage.success('删除成功'); load(); } }).catch(() => {});
        onMounted(() => { load(); loadCoaches(); });
        return { list, total, loading, query, dlgVisible, form, coachList, canWrite, isAdmin, load, showForm, save, del };
    }
};

// ============================================================
// 签到管理（ADMIN/COACH）
// ============================================================
const CheckInTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/treadmill.svg" />签到管理</h2><div><el-button type="success" @click="showCheckIn">会员签到</el-button><el-button type="warning" @click="showCheckOut">会员签退</el-button></div></div>
        <el-row :gutter="16" style="margin-bottom:16px"><el-col :span="6"><el-statistic title="今日签到" :value="todayCount" /></el-col><el-col :span="6"><el-statistic title="当前在场" :value="currentCount" /></el-col></el-row>
        <div class="search-bar">
            <el-select v-model="query.memberId" placeholder="选择会员" clearable filterable remote :remote-method="searchMembers" :loading="memberLoading" style="width:240px" @change="load">
                <el-option v-for="m in memberOptions" :key="m.id" :label="m.name + ' ('+m.memberNo+')'" :value="m.id" />
            </el-select>
            <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable @change="load" />
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="memberId" label="会员ID" width="100" />
            <el-table-column label="会员姓名" width="140"><template #default="{row}">{{memberMap[row.memberId]?.name||'-'}}</template></el-table-column>
            <el-table-column prop="checkInTime" label="进场时间" />
            <el-table-column prop="checkOutTime" label="离场时间"><template #default="{row}">{{row.checkOutTime||'在场中'}}</template></el-table-column>
            <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
            <el-table-column prop="gate" label="通道" width="100" />
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" title="会员签到" width="420px">
            <el-form label-width="90px">
                <el-form-item label="会员">
                    <el-select v-model="checkForm.memberId" filterable remote :remote-method="searchMembers" :loading="memberLoading" placeholder="搜索会员姓名/手机/编号" style="width:100%">
                        <el-option v-for="m in memberOptions" :key="m.id" :label="m.name + ' ('+m.memberNo+')'" :value="m.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="通道"><el-input v-model="checkForm.gate" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="doCheckIn">确认签到</el-button></template>
        </el-dialog>
        <el-dialog v-model="outVisible" title="会员签退" width="420px">
            <el-form label-width="90px">
                <el-form-item label="会员">
                    <el-select v-model="outMemberId" filterable remote :remote-method="searchMembers" :loading="memberLoading" placeholder="搜索会员" style="width:100%">
                        <el-option v-for="m in memberOptions" :key="m.id" :label="m.name + ' ('+m.memberNo+')'" :value="m.id" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer><el-button @click="outVisible=false">取消</el-button><el-button type="primary" @click="doCheckOut">确认签退</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, memberId: null, date: null });
        const dlgVisible = ref(false); const checkForm = reactive({ memberId: null, gate: 'GATE-01' });
        const outVisible = ref(false); const outMemberId = ref(null);
        const todayCount = ref(0); const currentCount = ref(0);
        const memberOptions = ref([]); const memberLoading = ref(false); const memberMap = ref({});

        const load = async () => {
            loading.value = true;
            try {
                const res = await api.get('/checkin/list', { params: query });
                if (res.code === 200) {
                    list.value = res.data.records;
                    total.value = res.data.total;
                    // 拉一下涉及到的会员姓名
                    const ids = [...new Set(list.value.map(r => r.memberId))].filter(i => !memberMap.value[i]);
                    if (ids.length) {
                        const proms = ids.map(id => api.get('/member/' + id).catch(() => null));
                        (await Promise.all(proms)).forEach(r => { if (r && r.code === 200) memberMap.value[r.data.id] = r.data; });
                    }
                }
            } finally { loading.value = false; }
        };
        const loadCounts = async () => {
            const today = new Date().toISOString().split('T')[0];
            const [tRes, cRes] = await Promise.all([
                api.get('/checkin/list', { params: { page: 1, size: 1, date: today } }),
                api.get('/checkin/current')
            ]);
            if (tRes.code === 200) todayCount.value = tRes.data.total;
            if (cRes.code === 200) currentCount.value = cRes.data;
        };
        const searchMembers = async kw => {
            memberLoading.value = true;
            try { const res = await api.get('/member/list', { params: { page: 1, size: 20, keyword: kw } }); if (res.code === 200) memberOptions.value = res.data.records; }
            finally { memberLoading.value = false; }
        };
        const showCheckIn = () => { checkForm.memberId = null; checkForm.gate = 'GATE-01'; memberOptions.value = []; dlgVisible.value = true; };
        const showCheckOut = () => { outMemberId.value = null; memberOptions.value = []; outVisible.value = true; };
        const doCheckIn = async () => {
            if (!checkForm.memberId) return ElMessage.warning('请选择会员');
            const res = await api.post('/checkin/in', null, { params: checkForm });
            if (res.code === 200) { ElMessage.success('签到成功'); dlgVisible.value = false; load(); loadCounts(); }
        };
        const doCheckOut = async () => {
            if (!outMemberId.value) return ElMessage.warning('请选择会员');
            const res = await api.post('/checkin/out', null, { params: { memberId: outMemberId.value } });
            if (res.code === 200) { ElMessage.success('签退成功'); outVisible.value = false; load(); loadCounts(); }
        };
        onMounted(() => { load(); loadCounts(); searchMembers(''); });
        return { list, total, loading, query, dlgVisible, checkForm, outVisible, outMemberId, todayCount, currentCount, memberOptions, memberLoading, memberMap, load, showCheckIn, showCheckOut, doCheckIn, doCheckOut, searchMembers };
    }
};

// ============================================================
// 器材管理（ADMIN 写；所有角色读）
// ============================================================
const EquipmentTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/treadmill.svg" />器材管理</h2><el-button v-if="isAdmin" type="primary" @click="showForm()">新增器材</el-button></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="设备名称" clearable style="width:200px" @keyup.enter="load" />
            <el-select v-model="query.category" placeholder="分类" clearable style="width:120px" @change="load"><el-option v-for="c in ['有氧','力量','自由重量','功能训练']" :key="c" :label="c" :value="c" /></el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="load"><el-option label="正常" :value="1" /><el-option label="维修中" :value="2" /><el-option label="停用" :value="3" /><el-option label="报废" :value="0" /></el-select>
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="name" label="设备名称" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="brand" label="品牌" width="120" />
            <el-table-column prop="location" label="位置" width="130" />
            <el-table-column prop="purchaseDate" label="购入日期" width="110" />
            <el-table-column prop="price" label="价格" width="100"><template #default="{row}">¥{{fmtMoney(row.price)}}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="['danger','','warning','info'][row.status]" size="small">{{['报废','正常','维修中','停用'][row.status]}}</el-tag></template></el-table-column>
            <el-table-column prop="nextMaintenance" label="下次保养" width="110" />
            <el-table-column v-if="isAdmin" label="操作" width="160">
                <template #default="{row}"><el-button size="small" @click="showForm(row)">编辑</el-button><el-button size="small" type="danger" @click="del(row)">删除</el-button></template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" :title="form.id?'编辑器材':'新增器材'" width="600px">
            <el-form :model="form" label-width="90px">
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item></el-col><el-col :span="12"><el-form-item label="分类"><el-select v-model="form.category" style="width:100%"><el-option v-for="c in ['有氧','力量','自由重量','功能训练']" :key="c" :label="c" :value="c" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="8"><el-form-item label="品牌"><el-input v-model="form.brand" /></el-form-item></el-col><el-col :span="8"><el-form-item label="型号"><el-input v-model="form.model" /></el-form-item></el-col><el-col :span="8"><el-form-item label="位置"><el-input v-model="form.location" /></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="8"><el-form-item label="购入日期"><el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col><el-col :span="8"><el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col><el-col :span="8"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="正常" :value="1" /><el-option label="维修中" :value="2" /><el-option label="停用" :value="3" /><el-option label="报废" :value="0" /></el-select></el-form-item></el-col></el-row>
                <el-row :gutter="16"><el-col :span="12"><el-form-item label="上次保养"><el-date-picker v-model="form.lastMaintenance" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="下次保养"><el-date-picker v-model="form.nextMaintenance" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col></el-row>
                <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, keyword: '', category: '', status: null });
        const dlgVisible = ref(false); const form = reactive({});
        const isAdmin = computed(() => safeUser().role === 'ADMIN');
        const load = async () => { loading.value = true; try { const res = await api.get('/equipment/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        const showForm = row => { Object.assign(form, row ? { ...row } : { name: '', category: '有氧', brand: '', model: '', location: '', purchaseDate: '', price: 0, status: 1, lastMaintenance: '', nextMaintenance: '', remark: '' }); dlgVisible.value = true; };
        const save = async () => { const res = form.id ? await api.put('/equipment/' + form.id, form) : await api.post('/equipment', form); if (res.code === 200) { ElMessage.success(res.message); dlgVisible.value = false; load(); } };
        const del = row => ElMessageBox.confirm('确定删除该器材？', '提示', { type: 'warning' }).then(async () => { const res = await api.delete('/equipment/' + row.id); if (res.code === 200) { ElMessage.success('删除成功'); load(); } }).catch(() => {});
        onMounted(load);
        return { list, total, loading, query, dlgVisible, form, isAdmin, load, showForm, save, del, fmtMoney };
    }
};

// ============================================================
// 财务管理（ADMIN）
// ============================================================
const PaymentTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/wallet.svg" />财务管理</h2></div>
        <div class="search-bar">
            <el-select v-model="query.memberId" placeholder="选择会员" clearable filterable remote :remote-method="searchMembers" :loading="memberLoading" style="width:240px" @change="load">
                <el-option v-for="m in memberOptions" :key="m.id" :label="m.name + ' ('+m.memberNo+')'" :value="m.id" />
            </el-select>
            <el-select v-model="query.type" placeholder="类型" clearable style="width:120px" @change="load"><el-option v-for="t in ['购卡','续费','私教','课程','商品','退款']" :key="t" :label="t" :value="t" /></el-select>
            <el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" clearable @change="load" />
            <el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" clearable @change="load" />
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="memberId" label="会员ID" width="90" />
            <el-table-column label="会员" width="120"><template #default="{row}">{{memberMap[row.memberId]?.name||'-'}}</template></el-table-column>
            <el-table-column prop="type" label="类型" width="80"><template #default="{row}"><el-tag size="small">{{row.type}}</el-tag></template></el-table-column>
            <el-table-column prop="amount" label="金额" width="120"><template #default="{row}"><span :class="row.amount>=0?'amount-positive':'amount-negative'">{{row.amount>=0?'+':''}}¥{{row.amount}}</span></template></el-table-column>
            <el-table-column prop="payMethod" label="支付方式" width="100" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, memberId: null, type: '', startDate: null, endDate: null });
        const memberOptions = ref([]); const memberLoading = ref(false); const memberMap = ref({});
        const load = async () => {
            loading.value = true;
            try {
                const res = await api.get('/payment/list', { params: query });
                if (res.code === 200) {
                    list.value = res.data.records; total.value = res.data.total;
                    const ids = [...new Set(list.value.map(r => r.memberId))].filter(i => i && !memberMap.value[i]);
                    if (ids.length) {
                        const proms = ids.map(id => api.get('/member/' + id).catch(() => null));
                        (await Promise.all(proms)).forEach(r => { if (r && r.code === 200) memberMap.value[r.data.id] = r.data; });
                    }
                }
            } finally { loading.value = false; }
        };
        const searchMembers = async kw => {
            memberLoading.value = true;
            try { const res = await api.get('/member/list', { params: { page: 1, size: 20, keyword: kw } }); if (res.code === 200) memberOptions.value = res.data.records; }
            finally { memberLoading.value = false; }
        };
        onMounted(() => { load(); searchMembers(''); });
        return { list, total, loading, query, memberOptions, memberLoading, memberMap, load, searchMembers };
    }
};

// ============================================================
// 用户管理（ADMIN）
// ============================================================
const UserTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/person.svg" />用户管理</h2><el-button type="primary" @click="showForm()">新增用户</el-button></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="搜索用户名/姓名/手机" clearable style="width:250px" @keyup.enter="load" />
            <el-select v-model="query.role" placeholder="角色" clearable style="width:120px" @change="load"><el-option label="管理员" value="ADMIN" /><el-option label="教练" value="COACH" /><el-option label="会员" value="MEMBER" /></el-select>
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="phone" label="手机" />
            <el-table-column prop="role" label="角色" width="90"><template #default="{row}"><el-tag :type="row.role==='ADMIN'?'danger':row.role==='COACH'?'warning':''" size="small">{{row.role}}</el-tag></template></el-table-column>
            <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{row.status===1?'启用':'禁用'}}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="320">
                <template #default="{row}">
                    <el-button size="small" @click="showForm(row)">编辑</el-button>
                    <el-button size="small" type="warning" @click="resetPwd(row)">重置密码</el-button>
                    <el-button size="small" :type="row.status===1?'danger':'success'" @click="toggle(row)">{{row.status===1?'禁用':'启用'}}</el-button>
                    <el-button size="small" type="danger" @click="del(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" :title="form.id?'编辑用户':'新增用户'" width="500px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
                <el-form-item label="密码"><el-input v-model="form.password" type="password" :placeholder="form.id?'留空不修改':'请输入密码'" /></el-form-item>
                <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
                <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
                <el-form-item label="性别"><el-select v-model="form.gender" style="width:100%"><el-option label="男" :value="1" /><el-option label="女" :value="2" /><el-option label="未知" :value="0" /></el-select></el-form-item>
                <el-form-item label="角色"><el-select v-model="form.role" style="width:100%"><el-option label="ADMIN" value="ADMIN" /><el-option label="COACH" value="COACH" /><el-option label="MEMBER" value="MEMBER" /></el-select></el-form-item>
                <el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, keyword: '', role: '' });
        const dlgVisible = ref(false); const form = reactive({});
        const load = async () => { loading.value = true; try { const res = await api.get('/admin/user/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        const showForm = row => { Object.assign(form, row ? { ...row, password: '' } : { username: '', password: '', realName: '', phone: '', email: '', gender: 0, role: 'MEMBER', status: 1 }); dlgVisible.value = true; };
        const save = async () => {
            if (!form.id && !form.password) { ElMessage.warning('请输入密码'); return; }
            const res = form.id ? await api.put('/admin/user/' + form.id, form) : await api.post('/admin/user', form);
            if (res.code === 200) { ElMessage.success(res.message); dlgVisible.value = false; load(); }
        };
        const del = row => ElMessageBox.confirm('确定删除？同时会删除关联的会员/教练档案。', '提示', { type: 'warning' }).then(async () => { const res = await api.delete('/admin/user/' + row.id); if (res.code === 200) { ElMessage.success('删除成功'); load(); } }).catch(() => {});
        const resetPwd = row => ElMessageBox.confirm('重置密码为 123456？', '提示').then(async () => { const res = await api.post('/admin/user/' + row.id + '/reset-password'); if (res.code === 200) ElMessage.success('已重置为 123456'); }).catch(() => {});
        const toggle = async row => { const res = await api.post('/admin/user/' + row.id + '/toggle-status'); if (res.code === 200) { ElMessage.success(res.message); load(); } };
        onMounted(load);
        return { list, total, loading, query, dlgVisible, form, load, showForm, save, del, resetPwd, toggle };
    }
};

// ============================================================
// 通知管理（ADMIN，发送 + 群发 + 列表）
// ============================================================
const NotificationAdminTemplate = {
    template: `
    <div>
        <div class="page-header">
            <h2><img src="/icons/bell.svg" />通知管理</h2>
            <div>
                <el-button type="primary" @click="showForm()">单发通知</el-button>
                <el-button type="warning" @click="bcVisible=true">群发通知</el-button>
            </div>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="userId" label="接收人ID" width="100" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="type" label="类型" width="100"><template #default="{row}"><el-tag :type="row.type==='SYSTEM'?'danger':row.type==='PROMOTION'?'warning':''" size="small">{{row.type}}</el-tag></template></el-table-column>
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column prop="isRead" label="状态" width="80"><template #default="{row}"><el-tag :type="row.isRead?'info':'danger'" size="small">{{row.isRead?'已读':'未读'}}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
        <el-dialog v-model="dlgVisible" title="单发通知" width="520px">
            <el-form :model="form" label-width="100px">
                <el-form-item label="接收人">
                    <el-select v-model="form.userId" filterable remote :remote-method="searchUsers" :loading="userLoading" placeholder="搜索用户名/姓名" style="width:100%">
                        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName + ' ('+u.username+')'" :value="u.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
                <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
                <el-form-item label="类型"><el-select v-model="form.type" style="width:100%"><el-option label="系统通知" value="SYSTEM" /><el-option label="提醒" value="REMINDER" /><el-option label="活动推广" value="PROMOTION" /></el-select></el-form-item>
            </el-form>
            <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="send">发送</el-button></template>
        </el-dialog>
        <el-dialog v-model="bcVisible" title="群发通知" width="520px">
            <el-form :model="bcForm" label-width="100px">
                <el-form-item label="目标群体"><el-select v-model="bcForm.role" style="width:100%"><el-option label="全部学员" value="MEMBER" /><el-option label="全部教练" value="COACH" /><el-option label="全部管理员" value="ADMIN" /></el-select></el-form-item>
                <el-form-item label="标题"><el-input v-model="bcForm.title" /></el-form-item>
                <el-form-item label="内容"><el-input v-model="bcForm.content" type="textarea" :rows="4" /></el-form-item>
                <el-form-item label="类型"><el-select v-model="bcForm.type" style="width:100%"><el-option label="系统通知" value="SYSTEM" /><el-option label="提醒" value="REMINDER" /><el-option label="活动推广" value="PROMOTION" /></el-select></el-form-item>
            </el-form>
            <template #footer><el-button @click="bcVisible=false">取消</el-button><el-button type="warning" @click="broadcast">立即群发</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10 });
        const dlgVisible = ref(false); const form = reactive({});
        const bcVisible = ref(false); const bcForm = reactive({ role: 'MEMBER', title: '', content: '', type: 'SYSTEM' });
        const userOptions = ref([]); const userLoading = ref(false);
        const load = async () => { loading.value = true; try { const res = await api.get('/notification/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        const searchUsers = async kw => { userLoading.value = true; try { const res = await api.get('/admin/user/list', { params: { page: 1, size: 20, keyword: kw } }); if (res.code === 200) userOptions.value = res.data.records; } finally { userLoading.value = false; } };
        const showForm = () => { Object.assign(form, { userId: null, title: '', content: '', type: 'SYSTEM' }); userOptions.value = []; dlgVisible.value = true; };
        const send = async () => {
            if (!form.userId || !form.title || !form.content) return ElMessage.warning('请填写完整');
            const res = await api.post('/notification', form);
            if (res.code === 200) { ElMessage.success('发送成功'); dlgVisible.value = false; load(); }
        };
        const broadcast = async () => {
            if (!bcForm.title || !bcForm.content) return ElMessage.warning('请填写完整');
            const res = await api.post('/notification/broadcast', bcForm);
            if (res.code === 200) { ElMessage.success(res.message); bcVisible.value = false; load(); }
        };
        onMounted(() => { load(); searchUsers(''); });
        return { list, total, loading, query, dlgVisible, form, bcVisible, bcForm, userOptions, userLoading, load, searchUsers, showForm, send, broadcast };
    }
};


// ============================================================
// 操作日志（ADMIN）
// ============================================================
const SysLogTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/person.svg" />操作日志</h2></div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column prop="operation" label="操作" />
            <el-table-column prop="method" label="方法" />
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10 });
        const load = async () => { loading.value = true; try { const res = await api.get('/admin/log/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        onMounted(load);
        return { list, total, loading, query, load };
    }
};

// ============================================================
// 教练侧 - 我的课程
// ============================================================
const CoachMyCoursesTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/calendar.svg" />我的课程</h2></div>
        <div v-if="!list.length && !loading" class="empty-state">
            <img src="/icons/calendar.svg" />
            <p>暂无您的课程，请联系管理员安排课表</p>
        </div>
        <div v-else class="course-grid">
            <div v-for="c in list" :key="c.id" class="course-card">
                <div class="title">
                    {{c.name}}
                    <el-tag :type="['info','','success','info'][c.status]" size="small">{{['已取消','报名中','进行中','已结束'][c.status]}}</el-tag>
                </div>
                <div class="info">
                    <span><b>类型:</b> {{c.type}}</span>
                    <span><b>分类:</b> {{c.category}}</span><br />
                    <span><b>教室:</b> {{c.room}}</span><br />
                    <span><b>时间:</b> {{c.startTime}} ~ {{c.endTime}}</span>
                </div>
                <div class="footer">
                    <div class="capacity-bar"><div class="fill" :style="{width: ((c.currentCount||0)/(c.maxCapacity||1)*100)+'%'}"></div></div>
                    <span>{{c.currentCount}}/{{c.maxCapacity}}</span>
                    <el-button size="small" type="primary" @click="viewReservations(c)">名单</el-button>
                </div>
            </div>
        </div>
        <el-drawer v-model="drawerVisible" :title="'《'+(currentCourse?.name||'')+'》预约名单'" size="50%">
            <el-table :data="reservations" v-loading="rLoading" size="small">
                <el-table-column prop="memberId" label="会员ID" width="90" />
                <el-table-column label="会员姓名"><template #default="{row}">{{memberMap[row.memberId]?.name||'-'}}</template></el-table-column>
                <el-table-column label="手机"><template #default="{row}">{{memberMap[row.memberId]?.phone||'-'}}</template></el-table-column>
                <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="reservationTagType(row.status)" size="small">{{reservationStatusText(row.status)}}</el-tag></template></el-table-column>
                <el-table-column prop="reserveTime" label="预约时间" width="170" />
                <el-table-column label="操作" width="120">
                    <template #default="{row}"><el-button v-if="row.status===1" size="small" type="success" @click="checkIn(row)">签到</el-button></template>
                </el-table-column>
            </el-table>
        </el-drawer>
    </div>`,
    setup() {
        const list = ref([]); const loading = ref(false);
        const drawerVisible = ref(false); const currentCourse = ref(null);
        const reservations = ref([]); const rLoading = ref(false);
        const memberMap = ref({});
        const load = async () => { loading.value = true; try { const res = await api.get('/me/courses'); if (res.code === 200) list.value = res.data; } finally { loading.value = false; } };
        const viewReservations = async course => {
            currentCourse.value = course; drawerVisible.value = true;
            rLoading.value = true;
            try {
                const res = await api.get('/me/courses/reservations', { params: { courseId: course.id } });
                if (res.code === 200) {
                    reservations.value = res.data;
                    const ids = [...new Set(res.data.map(r => r.memberId))].filter(i => !memberMap.value[i]);
                    if (ids.length) {
                        const proms = ids.map(id => api.get('/member/' + id).catch(() => null));
                        (await Promise.all(proms)).forEach(r => { if (r && r.code === 200) memberMap.value[r.data.id] = r.data; });
                    }
                }
            } finally { rLoading.value = false; }
        };
        const checkIn = async row => {
            const res = await api.post('/course/reservation/' + row.id + '/checkin');
            if (res.code === 200) { ElMessage.success('签到成功'); viewReservations(currentCourse.value); }
        };
        onMounted(load);
        return { list, loading, drawerVisible, currentCourse, reservations, rLoading, memberMap, viewReservations, checkIn, reservationStatusText, reservationTagType };
    }
};

// ============================================================
// 学员侧 - 课程浏览 + 预约
// ============================================================
const MemberBrowseCoursesTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/calendar.svg" />可预约的课程</h2></div>
        <div class="search-bar">
            <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width:200px" @keyup.enter="load" />
            <el-select v-model="query.type" placeholder="类型" clearable style="width:120px" @change="load"><el-option v-for="t in ['团课','私教','小班']" :key="t" :label="t" :value="t" /></el-select>
            <el-select v-model="query.category" placeholder="分类" clearable style="width:120px" @change="load"><el-option v-for="c in ['有氧','力量','柔韧','格斗','舞蹈']" :key="c" :label="c" :value="c" /></el-select>
            <el-button type="primary" @click="load">搜索</el-button>
        </div>
        <div v-if="!list.length && !loading" class="empty-state">
            <img src="/icons/calendar.svg" />
            <p>暂无符合条件的课程</p>
        </div>
        <div v-else class="course-grid">
            <div v-for="c in list" :key="c.id" class="course-card">
                <div class="title">
                    {{c.name}}
                    <el-tag :type="['info','','success','info'][c.status]" size="small">{{['已取消','报名中','进行中','已结束'][c.status]}}</el-tag>
                </div>
                <div class="info">
                    <el-tag size="small">{{c.type}}</el-tag>
                    <el-tag size="small" effect="plain" style="margin-left:6px">{{c.category}}</el-tag>
                    <div style="margin-top:8px">
                        <span><b>教室:</b> {{c.room}}</span><br />
                        <span><b>时间:</b> {{c.startTime}}</span><br />
                        <span v-if="c.description"><b>简介:</b> {{c.description}}</span>
                    </div>
                </div>
                <div class="footer">
                    <span :class="['price-tag', c.price>0?'':'free']">{{c.price>0?'¥'+c.price:'免费'}}</span>
                    <div class="capacity-bar"><div class="fill" :style="{width: ((c.currentCount||0)/(c.maxCapacity||1)*100)+'%'}"></div></div>
                    <el-button size="small" type="primary" :disabled="c.status!==1 || (c.currentCount>=c.maxCapacity)" @click="reserve(c)">{{c.currentCount>=c.maxCapacity?'已满员':'立即预约'}}</el-button>
                </div>
            </div>
        </div>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 12, keyword: '', type: '', category: '', status: 1 });
        const load = async () => {
            loading.value = true;
            try { const res = await api.get('/course/list', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } }
            finally { loading.value = false; }
        };
        const reserve = c => ElMessageBox.prompt('为《' + c.name + '》填写备注（可选）', '确认预约', { confirmButtonText: '预约', cancelButtonText: '取消', inputValue: '' }).then(async ({ value }) => {
            const res = await api.post('/me/reservations', null, { params: { courseId: c.id, remark: value || '' } });
            if (res.code === 200) { ElMessage.success('预约成功'); load(); }
        }).catch(() => {});
        onMounted(load);
        return { list, total, loading, query, load, reserve };
    }
};

// ============================================================
// 学员侧 - 我的预约
// ============================================================
const MyReservationsTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/calendar.svg" />我的预约</h2></div>
        <div class="search-bar">
            <el-select v-model="query.status" placeholder="状态" clearable style="width:140px" @change="load">
                <el-option label="已预约" :value="1" />
                <el-option label="已签到" :value="2" />
                <el-option label="已取消" :value="0" />
            </el-select>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column label="课程"><template #default="{row}">{{courseMap[row.courseId]?.name||'课程#'+row.courseId}}</template></el-table-column>
            <el-table-column label="开始时间"><template #default="{row}">{{courseMap[row.courseId]?.startTime||'-'}}</template></el-table-column>
            <el-table-column label="教室"><template #default="{row}">{{courseMap[row.courseId]?.room||'-'}}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="reservationTagType(row.status)" size="small">{{reservationStatusText(row.status)}}</el-tag></template></el-table-column>
            <el-table-column prop="reserveTime" label="预约时间" width="170" />
            <el-table-column label="操作" width="100">
                <template #default="{row}"><el-button v-if="row.status===1" size="small" type="danger" @click="cancel(row)">取消</el-button></template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10, status: null });
        const courseMap = ref({});
        const load = async () => {
            loading.value = true;
            try {
                const res = await api.get('/me/reservations', { params: query });
                if (res.code === 200) {
                    list.value = res.data.records; total.value = res.data.total;
                    const ids = [...new Set(list.value.map(r => r.courseId))].filter(i => !courseMap.value[i]);
                    if (ids.length) {
                        const proms = ids.map(id => api.get('/course/' + id).catch(() => null));
                        (await Promise.all(proms)).forEach(r => { if (r && r.code === 200) courseMap.value[r.data.id] = r.data; });
                    }
                }
            } finally { loading.value = false; }
        };
        const cancel = row => ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' }).then(async () => {
            const res = await api.post('/me/reservations/' + row.id + '/cancel');
            if (res.code === 200) { ElMessage.success('已取消'); load(); }
        }).catch(() => {});
        onMounted(load);
        return { list, total, loading, query, courseMap, load, cancel, reservationStatusText, reservationTagType };
    }
};

// ============================================================
// 学员侧 - 我的会员卡
// ============================================================
const MyCardsTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/wallet.svg" />我的会员卡</h2></div>
        <div v-if="!cards.length && !loading" class="empty-state">
            <img src="/icons/wallet.svg" />
            <p>您还没有会员卡，请到前台办理</p>
        </div>
        <div v-else class="card-list">
            <div v-for="c in cards" :key="c.id" :class="['member-card', c.status===3?'tier-frozen':cardTier(c.cardName)]">
                <div class="status-tag">{{cardStatusText(c.status)}}</div>
                <div class="card-name">{{c.cardName||c.cardType}}</div>
                <div class="card-type">{{c.cardType}}</div>
                <div class="price">¥{{c.price}}</div>
                <div class="card-row"><span>开始</span><span>{{c.startDate||'-'}}</span></div>
                <div class="card-row"><span>到期</span><span>{{c.endDate||'-'}}</span></div>
                <div v-if="c.remainingTimes!=null" class="card-row"><span>剩余次数</span><span>{{c.remainingTimes}}</span></div>
            </div>
        </div>
    </div>`,
    setup() {
        const cards = ref([]); const loading = ref(false);
        const load = async () => { loading.value = true; try { const res = await api.get('/me/cards'); if (res.code === 200) cards.value = res.data; } finally { loading.value = false; } };
        onMounted(load);
        return { cards, loading, cardStatusText, cardTier };
    }
};

// ============================================================
// 学员侧 - 我的签到记录
// ============================================================
const MyCheckinsTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/treadmill.svg" />我的签到记录</h2></div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="checkInTime" label="进场时间" />
            <el-table-column prop="checkOutTime" label="离场时间"><template #default="{row}">{{row.checkOutTime||'在场中'}}</template></el-table-column>
            <el-table-column prop="durationMin" label="时长(分钟)" width="120" />
            <el-table-column prop="gate" label="通道" width="100" />
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10 });
        const load = async () => { loading.value = true; try { const res = await api.get('/me/checkins', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        onMounted(load);
        return { list, total, loading, query, load };
    }
};

// ============================================================
// 学员侧 - 我的消费
// ============================================================
const MyPaymentsTemplate = {
    template: `
    <div>
        <div class="page-header"><h2><img src="/icons/wallet.svg" />我的消费记录</h2></div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="type" label="类型" width="100"><template #default="{row}"><el-tag size="small">{{row.type}}</el-tag></template></el-table-column>
            <el-table-column prop="amount" label="金额" width="140"><template #default="{row}"><span :class="row.amount>=0?'amount-positive':'amount-negative'">{{row.amount>=0?'+':''}}¥{{row.amount}}</span></template></el-table-column>
            <el-table-column prop="payMethod" label="支付方式" width="100" />
            <el-table-column prop="remark" label="备注" />
            <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
    </div>`,
    setup() {
        const list = ref([]); const loading = ref(false);
        const load = async () => { loading.value = true; try { const res = await api.get('/me/payments'); if (res.code === 200) list.value = res.data; } finally { loading.value = false; } };
        onMounted(load);
        return { list, loading };
    }
};

// ============================================================
// 学员/教练 - 个人资料
// ============================================================
const ProfileTemplate = {
    template: `
    <div>
        <div class="portal-hero">
            <div class="avatar">{{(user.realName||user.username||'U').charAt(0).toUpperCase()}}</div>
            <div style="flex:1">
                <h2>{{user.realName||user.username}}</h2>
                <p>{{role==='ADMIN'?'系统管理员':role==='COACH'?'金牌教练':'尊敬的会员'}}</p>
                <div class="meta">
                    <span><b>账号：</b>{{user.username}}</span>
                    <span v-if="user.phone"><b>手机：</b>{{user.phone}}</span>
                    <span v-if="user.email"><b>邮箱：</b>{{user.email}}</span>
                </div>
            </div>
            <div>
                <el-button type="primary" @click="pwdVisible=true">修改密码</el-button>
            </div>
        </div>

        <el-card v-if="role==='MEMBER' && memberProfile" header="会员档案">
            <el-form :model="memberForm" label-width="100px">
                <el-row :gutter="16">
                    <el-col :span="8"><el-form-item label="会员编号"><el-input :model-value="memberProfile.memberNo" disabled /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="姓名"><el-input v-model="memberForm.name" /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="性别"><el-select v-model="memberForm.gender" style="width:100%"><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item></el-col>
                </el-row>
                <el-row :gutter="16">
                    <el-col :span="8"><el-form-item label="手机"><el-input v-model="memberForm.phone" /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="生日"><el-date-picker v-model="memberForm.birthday" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="紧急联系人"><el-input v-model="memberForm.emergencyContact" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="16">
                    <el-col :span="8"><el-form-item label="紧急电话"><el-input v-model="memberForm.emergencyPhone" /></el-form-item></el-col>
                    <el-col :span="16"><el-form-item label="健康备注"><el-input v-model="memberForm.healthNote" type="textarea" :rows="2" /></el-form-item></el-col>
                </el-row>
                <el-form-item><el-button type="primary" @click="saveMember">保存</el-button></el-form-item>
            </el-form>
        </el-card>

        <el-card v-if="role==='COACH' && coachProfile" header="教练档案">
            <el-form :model="coachForm" label-width="100px">
                <el-row :gutter="16">
                    <el-col :span="8"><el-form-item label="教练编号"><el-input :model-value="coachProfile.coachNo" disabled /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="姓名"><el-input v-model="coachForm.name" /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="性别"><el-select v-model="coachForm.gender" style="width:100%"><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item></el-col>
                </el-row>
                <el-row :gutter="16">
                    <el-col :span="8"><el-form-item label="手机"><el-input v-model="coachForm.phone" /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="等级"><el-input :model-value="coachProfile.level" disabled /></el-form-item></el-col>
                    <el-col :span="8"><el-form-item label="课时费"><el-input :model-value="coachProfile.hourlyRate" disabled /></el-form-item></el-col>
                </el-row>
                <el-form-item label="擅长"><el-input v-model="coachForm.specialty" /></el-form-item>
                <el-form-item label="资质"><el-input v-model="coachForm.certification" /></el-form-item>
                <el-form-item label="个人简介"><el-input v-model="coachForm.bio" type="textarea" :rows="3" /></el-form-item>
                <el-form-item><el-button type="primary" @click="saveCoach">保存</el-button></el-form-item>
            </el-form>
        </el-card>

        <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
            <el-form :model="pwdForm" label-width="80px">
                <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
                <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
                <el-form-item label="确认"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
            </el-form>
            <template #footer><el-button @click="pwdVisible=false">取消</el-button><el-button type="primary" @click="changePwd">提交</el-button></template>
        </el-dialog>
    </div>`,
    setup() {
        const user = ref({});
        const role = ref('');
        const memberProfile = ref(null); const coachProfile = ref(null);
        const memberForm = reactive({}); const coachForm = reactive({});
        const pwdVisible = ref(false); const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' });

        const load = async () => {
            const res = await api.get('/me/profile');
            if (res.code === 200) {
                user.value = res.data.user;
                role.value = res.data.user.role;
                if (res.data.memberProfile) {
                    memberProfile.value = res.data.memberProfile;
                    Object.assign(memberForm, res.data.memberProfile);
                }
                if (res.data.coachProfile) {
                    coachProfile.value = res.data.coachProfile;
                    Object.assign(coachForm, res.data.coachProfile);
                }
            }
        };
        const saveMember = async () => {
            const res = await api.put('/me/member-profile', memberForm);
            if (res.code === 200) { ElMessage.success('保存成功'); load(); }
        };
        const saveCoach = async () => {
            const res = await api.put('/me/coach-profile', coachForm);
            if (res.code === 200) { ElMessage.success('保存成功'); load(); }
        };
        const changePwd = async () => {
            if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请填写完整');
            if (pwdForm.newPassword !== pwdForm.confirm) return ElMessage.warning('两次输入不一致');
            if (pwdForm.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位');
            const res = await api.post('/auth/change-password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword });
            if (res.code === 200) {
                ElMessage.success('修改成功，请重新登录');
                pwdVisible.value = false;
                setTimeout(() => { localStorage.clear(); location.reload(); }, 1000);
            }
        };
        onMounted(load);
        return { user, role, memberProfile, coachProfile, memberForm, coachForm, pwdVisible, pwdForm, saveMember, saveCoach, changePwd };
    }
};

// ============================================================
// 我的通知（所有角色共用）
// ============================================================
const MyNotificationsTemplate = {
    template: `
    <div>
        <div class="page-header">
            <h2><img src="/icons/bell.svg" />我的通知</h2>
            <el-button @click="markAll">全部标记已读</el-button>
        </div>
        <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="title" label="标题">
                <template #default="{row}"><span :style="{fontWeight: row.isRead?400:600}">{{row.title}}</span></template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100"><template #default="{row}"><el-tag :type="row.type==='SYSTEM'?'danger':row.type==='PROMOTION'?'warning':''" size="small">{{row.type}}</el-tag></template></el-table-column>
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column prop="isRead" label="状态" width="80"><template #default="{row}"><el-tag :type="row.isRead?'info':'danger'" size="small">{{row.isRead?'已读':'未读'}}</el-tag></template></el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column label="操作" width="100">
                <template #default="{row}"><el-button v-if="!row.isRead" size="small" type="primary" @click="markRead(row)">标记已读</el-button></template>
            </el-table-column>
        </el-table>
        <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load" />
    </div>`,
    setup() {
        const list = ref([]); const total = ref(0); const loading = ref(false);
        const query = reactive({ page: 1, size: 10 });
        const load = async () => { loading.value = true; try { const res = await api.get('/notification/my', { params: query }); if (res.code === 200) { list.value = res.data.records; total.value = res.data.total; } } finally { loading.value = false; } };
        const markRead = async row => { const res = await api.post('/notification/' + row.id + '/read'); if (res.code === 200) { row.isRead = 1; } };
        const markAll = async () => { const res = await api.post('/notification/read-all'); if (res.code === 200) { ElMessage.success('已全部标记已读'); load(); } };
        onMounted(load);
        return { list, total, loading, query, load, markRead, markAll };
    }
};


// ============================================================
// 主框架（根据角色渲染不同侧边栏）
// ============================================================
const ADMIN_MENUS = [
    { key: 'dashboard', label: '数据看板', icon: '/icons/fitness.svg' },
    { key: 'member', label: '会员管理', icon: '/icons/person.svg' },
    { key: 'coach', label: '教练管理', icon: '/icons/person.svg' },
    { key: 'course', label: '课程管理', icon: '/icons/calendar.svg' },
    { key: 'checkin', label: '签到管理', icon: '/icons/treadmill.svg' },
    { key: 'equipment', label: '器材管理', icon: '/icons/treadmill.svg' },
    { key: 'payment', label: '财务管理', icon: '/icons/wallet.svg' },
    { key: 'notification-admin', label: '通知管理', icon: '/icons/bell.svg' },
    { key: 'user', label: '用户管理', icon: '/icons/person.svg' },
    { key: 'syslog', label: '操作日志', icon: '/icons/person.svg' },
];

const COACH_MENUS = [
    { key: 'dashboard', label: '数据看板', icon: '/icons/fitness.svg' },
    { key: 'my-courses', label: '我的课程', icon: '/icons/calendar.svg' },
    { key: 'course', label: '所有课程', icon: '/icons/calendar.svg' },
    { key: 'member', label: '会员档案', icon: '/icons/person.svg' },
    { key: 'checkin', label: '签到管理', icon: '/icons/treadmill.svg' },
    { key: 'my-notifications', label: '我的通知', icon: '/icons/bell.svg' },
    { key: 'profile', label: '个人资料', icon: '/icons/person.svg' },
];

const MEMBER_MENUS = [
    { key: 'browse-courses', label: '课程浏览', icon: '/icons/calendar.svg' },
    { key: 'my-reservations', label: '我的预约', icon: '/icons/calendar.svg' },
    { key: 'my-cards', label: '我的会员卡', icon: '/icons/wallet.svg' },
    { key: 'my-checkins', label: '我的签到', icon: '/icons/treadmill.svg' },
    { key: 'my-payments', label: '我的消费', icon: '/icons/wallet.svg' },
    { key: 'equipment', label: '器材一览', icon: '/icons/treadmill.svg' },
    { key: 'my-notifications', label: '我的通知', icon: '/icons/bell.svg' },
    { key: 'profile', label: '个人资料', icon: '/icons/person.svg' },
];

const ROLE_DEFAULT = { ADMIN: 'dashboard', COACH: 'dashboard', MEMBER: 'browse-courses' };

const AppTemplate = {
    template: `
    <el-container class="layout-container">
        <el-aside :width="collapsed?'64px':'220px'" :class="['layout-aside', collapsed?'is-collapsed':'']">
            <div class="logo-container">
                <img src="/icons/fitness.svg" class="logo-img" />
                <span v-show="!collapsed">健身房管理</span>
            </div>
            <el-menu :default-active="activeMenu" :collapse="collapsed" background-color="transparent" text-color="rgba(255,255,255,0.65)" active-text-color="#409eff" @select="handleMenu">
                <el-menu-item v-for="m in menus" :key="m.key" :index="m.key">
                    <img :src="m.icon" class="menu-icon" />
                    <span>{{m.label}}</span>
                </el-menu-item>
            </el-menu>
        </el-aside>
        <el-container>
            <el-header class="layout-header">
                <div class="header-left">
                    <div class="collapse-btn" @click="collapsed=!collapsed">
                        <svg viewBox="0 0 1024 1024" width="20" height="20"><path fill="currentColor" d="M128 256h768v64H128zm0 224h768v64H128zm0 224h768v64H128z"/></svg>
                    </div>
                    <span class="page-title">{{currentLabel}}</span>
                </div>
                <div class="user-info">
                    <div class="theme-toggle" @click="toggleTheme" :title="theme==='dark'?'切换到浅色':'切换到深色'">
                        <svg v-if="theme==='dark'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>
                        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
                    </div>
                    <el-popover placement="bottom-end" :width="320" trigger="click" @show="loadNotifs">
                        <template #reference>
                            <div class="notification-trigger">
                                <img src="/icons/bell.svg" />
                                <div class="dot" v-if="unreadCount>0"></div>
                            </div>
                        </template>
                        <div>
                            <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 12px;border-bottom:1px solid #f0f0f0">
                                <strong>消息中心</strong>
                                <el-button v-if="unreadCount>0" link type="primary" size="small" @click="markAllRead">全部已读</el-button>
                            </div>
                            <div class="notif-popover-list">
                                <div v-if="!notifList.length" style="padding:40px 20px;text-align:center;color:#909399">暂无消息</div>
                                <div v-for="n in notifList" :key="n.id" :class="['notif-item', !n.isRead?'unread':'']" @click="goNotifPage(n)">
                                    <div class="title">
                                        <span>{{n.title}}</span>
                                        <el-tag size="small" :type="n.type==='SYSTEM'?'danger':n.type==='PROMOTION'?'warning':''">{{n.type}}</el-tag>
                                    </div>
                                    <div class="content">{{n.content}}</div>
                                    <div class="time">{{n.createdAt}}</div>
                                </div>
                            </div>
                            <div style="text-align:center;padding:8px;border-top:1px solid #f0f0f0;cursor:pointer;color:#409eff" @click="activeMenu='my-notifications'">查看全部</div>
                        </div>
                    </el-popover>
                    <el-dropdown @command="handleCmd">
                        <span style="display:flex;align-items:center;gap:8px;cursor:pointer">
                            <el-avatar :size="32">{{(userName||'U').charAt(0).toUpperCase()}}</el-avatar>
                            <div style="display:flex;flex-direction:column">
                                <span style="font-weight:500;color:#303133;line-height:1.2">{{userName}}</span>
                                <span style="font-size:12px;color:#909399;line-height:1.2">{{roleLabel(userRole)}}</span>
                            </div>
                        </span>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </el-header>
            <el-main class="layout-main">
                <component :is="currentComponent" :key="activeMenu" />
            </el-main>
        </el-container>
    </el-container>`,
    setup() {
        const collapsed = ref(false);
        const user = safeUser();
        const userName = ref(user.realName || user.username || '用户');
        const userRole = ref(user.role || 'MEMBER');
        const menus = computed(() => ({ ADMIN: ADMIN_MENUS, COACH: COACH_MENUS, MEMBER: MEMBER_MENUS }[userRole.value] || MEMBER_MENUS));
        const activeMenu = ref(ROLE_DEFAULT[userRole.value] || 'profile');
        const currentLabel = computed(() => menus.value.find(m => m.key === activeMenu.value)?.label || '');

        const componentMap = {
            'dashboard': DashboardTemplate,
            'member': MemberTemplate,
            'coach': CoachTemplate,
            'course': CourseTemplate,
            'checkin': CheckInTemplate,
            'equipment': EquipmentTemplate,
            'payment': PaymentTemplate,
            'notification-admin': NotificationAdminTemplate,
            'user': UserTemplate,
            'syslog': SysLogTemplate,
            'my-courses': CoachMyCoursesTemplate,
            'browse-courses': MemberBrowseCoursesTemplate,
            'my-reservations': MyReservationsTemplate,
            'my-cards': MyCardsTemplate,
            'my-checkins': MyCheckinsTemplate,
            'my-payments': MyPaymentsTemplate,
            'my-notifications': MyNotificationsTemplate,
            'profile': ProfileTemplate,
        };
        const currentComponent = computed(() => componentMap[activeMenu.value] || ProfileTemplate);

        const handleMenu = idx => { activeMenu.value = idx; };
        Vue.provide('navigate', handleMenu);
        const handleCmd = cmd => {
            if (cmd === 'logout') { localStorage.clear(); location.reload(); }
            else if (cmd === 'profile') { activeMenu.value = 'profile'; }
        };

        // 通知 popover
        const unreadCount = ref(0);
        const notifList = ref([]);
        const loadUnread = async () => { try { const res = await api.get('/notification/unread-count'); if (res.code === 200) unreadCount.value = res.data; } catch (e) {} };
        const loadNotifs = async () => {
            try {
                const res = await api.get('/notification/my', { params: { page: 1, size: 10 } });
                if (res.code === 200) notifList.value = res.data.records;
            } catch (e) {}
        };
        const goNotifPage = async n => {
            if (!n.isRead) { try { await api.post('/notification/' + n.id + '/read'); n.isRead = 1; loadUnread(); } catch (e) {} }
        };
        const markAllRead = async () => {
            const res = await api.post('/notification/read-all');
            if (res.code === 200) { ElMessage.success('已全部标记已读'); notifList.value.forEach(n => n.isRead = 1); unreadCount.value = 0; }
        };

        // 主题切换：把 theme 持久化到 localStorage，HTML 根节点同步 data-theme/class
        const theme = ref(localStorage.getItem('theme') || 'light');
        const applyTheme = t => {
            document.documentElement.setAttribute('data-theme', t);
            if (t === 'dark') document.documentElement.classList.add('dark');
            else document.documentElement.classList.remove('dark');
        };
        applyTheme(theme.value);
        const toggleTheme = () => {
            theme.value = theme.value === 'dark' ? 'light' : 'dark';
            localStorage.setItem('theme', theme.value);
            applyTheme(theme.value);
        };

        onMounted(() => { loadUnread(); setInterval(loadUnread, 60_000); });
        return { collapsed, activeMenu, userName, userRole, menus, currentLabel, currentComponent, unreadCount, notifList, handleMenu, handleCmd, loadNotifs, goNotifPage, markAllRead, roleLabel, theme, toggleTheme };
    }
};

// ============================================================
// 应用入口
// ============================================================
const isLoggedIn = () => !!localStorage.getItem('token');
const RootApp = {
    setup() {
        return () => isLoggedIn() ? h(AppTemplate) : h(LoginTemplate);
    }
};

const app = Vue.createApp(RootApp);

// 全局错误处理，避免出错时页面完全空白
app.config.errorHandler = (err, instance, info) => {
    console.error('[Vue Error]', err, info);
    try { ElMessage.error('页面渲染异常: ' + (err.message || err)); } catch (e) {}
};
window.addEventListener('error', e => {
    console.error('[Global Error]', e.error || e.message);
});

app.use(ElementPlus);
app.mount('#app');
