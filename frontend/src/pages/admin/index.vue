<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <img :src="logo" alt="云睿学堂" />
        <div><b>云睿后台</b><span>运营与内容工作台</span></div>
      </div>
      <div v-if="authorized" class="side-caption">工作区</div>
      <nav v-if="authorized" class="side-nav">
        <button :class="{active: active==='dashboard'}" @click="switchView('dashboard')"><span>01</span>数据概览</button>
        <button :class="{active: active==='courses'}" @click="switchView('courses')"><span>02</span>课程与视频</button>
        <button :class="{active: active==='questions'}" @click="switchView('questions')"><span>03</span>问答与退款</button>
        <button :class="{active: active==='users'}" @click="switchView('users')"><span>04</span>用户管理</button>
        <button :class="{active: active==='quizzes'}" @click="switchView('quizzes')"><span>05</span>章节测验</button>
      </nav>
      <div class="side-bottom">
        <button @click="goStudent">← 返回学习端</button>
        <button v-if="authorized" @click="logout">退出管理员账号</button>
      </div>
    </aside>

    <main class="admin-main">
      <template v-if="!checked">
        <div class="loading-state">正在检查管理员权限…</div>
      </template>
      <template v-else-if="!authorized">
        <section class="admin-login">
          <div class="login-mark">Y</div>
          <p class="eyebrow">ADMIN ACCESS</p>
          <h1>进入云睿后台</h1>
          <p class="login-desc">课程发布、视频媒资、学习数据和用户服务集中管理。</p>
          <el-alert v-if="checked && hasToken" title="当前账号不是管理员，请使用管理员账号登录。" type="warning" :closable="false" show-icon />
          <el-form label-position="top" class="login-form" autocomplete="off" @submit.prevent="login">
            <el-form-item label="管理员账号"><el-input v-model="loginForm.username" autocomplete="username" placeholder="请输入账号" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="loginForm.password" type="password" autocomplete="new-password" show-password placeholder="请输入密码" @keyup.enter="login" /></el-form-item>
            <el-button type="primary" class="login-button" :loading="loginLoading" @click="login">登录后台</el-button>
          </el-form>
          <button class="back-student" @click="goStudent">返回学生端</button>
          <button v-if="hasToken" class="clear-session" @click="clearSession">清除当前账号，重新登录</button>
        </section>
      </template>
      <template v-else>
        <header class="admin-header">
          <div><p>管理后台 / {{ viewTitle }}</p><h1>{{ viewTitle }}</h1></div>
          <div class="header-account"><span>{{ profile.nickname || profile.username }}</span><el-avatar :src="avatar" /></div>
        </header>

        <section v-if="active==='dashboard'" class="view-content">
          <div class="stat-grid">
            <div class="stat-card stat-primary"><span>注册用户</span><strong>{{ dashboardData.users }}</strong><small>持续增长的学习人群</small></div>
            <div class="stat-card"><span>已发布课程</span><strong>{{ dashboardData.publishedCourses }}</strong><small>正在展示给学生</small></div>
            <div class="stat-card"><span>学习用户</span><strong>{{ dashboardData.learners }}</strong><small>产生过有效学习行为</small></div>
            <div class="stat-card"><span>有效收入</span><strong>￥{{ dashboardData.revenue }}</strong><small>已完成支付订单</small></div>
          </div>
          <div class="dashboard-grid">
            <section class="panel"><div class="panel-heading"><div><span class="eyebrow">COURSE PULSE</span><h2>课程销售排行</h2></div><span class="heading-note">按收入</span></div><div v-if="dashboardData.topCourses?.length" class="rank-list"><div v-for="(item,index) in dashboardData.topCourses" :key="item.title" class="rank-row"><b>{{ String(index+1).padStart(2,'0') }}</b><span>{{ item.title }}</span><strong>￥{{ item.revenue }}</strong></div></div><el-empty v-else description="暂无销售数据" /></section>
            <section class="panel insight-panel"><span class="eyebrow">待处理事项</span><h2>今天先处理这些</h2><div class="todo-row"><span>待审核退款</span><b>{{ dashboardData.pendingRefunds }}</b></div><div class="todo-row"><span>待回复问题</span><b>{{ dashboardData.openQuestions }}</b></div><p>进入“问答与退款”即可逐项处理，并保留处理结果。</p></section>
          </div>
        </section>

        <section v-if="active==='courses'" class="view-content">
          <div class="content-toolbar"><div><span class="eyebrow">CONTENT LIBRARY</span><h2>课程与视频</h2><p>维护课程状态、章节目录和可播放视频。</p></div><el-button type="primary" @click="courseDialog=true">新建课程</el-button></div>
          <section class="panel"><el-table :data="courseList" empty-text="暂无课程"><el-table-column prop="title" label="课程" min-width="240" /><el-table-column prop="category" label="分类" width="130" /><el-table-column prop="teacherName" label="讲师" width="130" /><el-table-column prop="price" label="价格" width="110"><template #default="s">￥{{ s.row.price }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="s"><el-tag :type="s.row.status==='PUBLISHED'?'success':'info'">{{ s.row.status==='PUBLISHED'?'已发布':'草稿' }}</el-tag></template></el-table-column><el-table-column label="操作" width="245"><template #default="s"><el-button size="small" @click="openSections(s.row)">章节与视频</el-button><el-button v-if="s.row.status!=='PUBLISHED'" size="small" type="primary" @click="changeStatus(s.row,'PUBLISHED')">发布</el-button><el-button v-else size="small" type="warning" @click="changeStatus(s.row,'OFFLINE')">下架</el-button></template></el-table-column></el-table></section>
        </section>

        <section v-if="active==='questions'" class="view-content">
          <div class="content-toolbar"><div><span class="eyebrow">SERVICE DESK</span><h2>问答与退款</h2><p>处理学生问题和售后申请，回复会进入学生消息中心。</p></div><el-button @click="loadServiceData">刷新</el-button></div>
          <div class="service-grid"><section class="panel"><div class="panel-heading"><h2>待回复问题</h2><span class="count-pill">{{ questionList.length }}</span></div><el-table :data="questionList" empty-text="没有待回复问题"><el-table-column prop="content" label="学生问题" min-width="250" /><el-table-column prop="courseId" label="课程" width="100" /><el-table-column label="操作" width="90"><template #default="s"><el-button type="primary" link @click="answerQuestion(s.row)">回复</el-button></template></el-table-column></el-table></section><section class="panel"><div class="panel-heading"><h2>待审核退款</h2><span class="count-pill">{{ refundList.length }}</span></div><el-table :data="refundList" empty-text="没有待审核退款"><el-table-column prop="orderId" label="订单" width="150" /><el-table-column prop="reason" label="原因" min-width="180" /><el-table-column label="操作" width="145"><template #default="s"><el-button size="small" type="success" @click="decideRefund(s.row,true)">通过</el-button><el-button size="small" type="danger" @click="decideRefund(s.row,false)">拒绝</el-button></template></el-table-column></el-table></section></div>
        </section>

        <section v-if="active==='users'" class="view-content"><div class="content-toolbar"><div><span class="eyebrow">PEOPLE</span><h2>用户管理</h2><p>搜索账号、调整状态和角色。</p></div><el-button @click="loadUsers">刷新</el-button></div><section class="panel"><div class="user-search"><el-input v-model="userQuery.keyword" clearable placeholder="搜索用户名或昵称" @keyup.enter="loadUsers" /><el-button type="primary" @click="loadUsers">搜索</el-button></div><el-table :data="userList" empty-text="暂无用户"><el-table-column prop="username" label="用户名" min-width="180" /><el-table-column prop="nickname" label="昵称" min-width="130" /><el-table-column prop="role" label="角色" width="110" /><el-table-column prop="status" label="状态" width="110" /><el-table-column label="操作" width="230"><template #default="s"><el-button size="small" @click="toggleUserStatus(s.row)">{{ s.row.status==='ACTIVE'?'停用':'启用' }}</el-button><el-button size="small" @click="toggleUserRole(s.row)">{{ s.row.role==='ADMIN'?'设为学生':'设为管理员' }}</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="userQuery.page" :page-size="userQuery.pageSize" :total="userTotal" layout="prev, pager, next" @current-change="loadUsers" /></section></section>

        <section v-if="active==='quizzes'" class="view-content"><div class="content-toolbar"><div><span class="eyebrow">ASSESSMENT</span><h2>章节测验</h2><p>为课程章节维护练习题和答案解析。</p></div></div><section class="panel quiz-panel"><div class="quiz-selects"><el-select v-model="quizCourseId" placeholder="选择课程" @change="loadQuizSections"><el-option v-for="c in courseList" :key="c.id" :label="c.title" :value="c.id" /></el-select><el-select v-model="quizSectionId" placeholder="选择章节" @change="loadQuizList"><el-option v-for="s in quizSections" :key="s.id" :label="s.title" :value="s.id" /></el-select><el-button type="primary" :disabled="!quizSectionId" @click="quizDialog=true">新建题目</el-button></div><el-table :data="quizList" empty-text="当前章节暂无题目"><el-table-column prop="question" label="题目" min-width="360" /><el-table-column prop="correctOption" label="答案" width="90" /><el-table-column label="状态" width="100"><template #default="s"><el-tag :type="s.row.active?'success':'info'">{{ s.row.active?'启用':'停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="90"><template #default="s"><el-button v-if="s.row.active" type="danger" link @click="disableQuiz(s.row)">停用</el-button></template></el-table-column></el-table></section></section>
      </template>
    </main>

    <el-dialog v-model="courseDialog" title="新建课程" width="520px"><el-form label-position="top"><el-form-item label="课程名称"><el-input v-model="courseForm.title" /></el-form-item><el-form-item label="分类"><el-input v-model="courseForm.category" /></el-form-item><el-form-item label="讲师"><el-input v-model="courseForm.teacherName" /></el-form-item><el-form-item label="价格"><el-input-number v-model="courseForm.price" :min="0" /></el-form-item><el-form-item label="介绍"><el-input v-model="courseForm.description" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="courseDialog=false">取消</el-button><el-button type="primary" @click="saveCourse">保存草稿</el-button></template></el-dialog>

    <el-dialog v-model="sectionDialog" :title="`章节与视频：${selectedCourse?.title || ''}`" width="760px"><el-form inline><el-form-item label="章节名称"><el-input v-model="sectionForm.title" /></el-form-item><el-form-item label="顺序"><el-input-number v-model="sectionForm.sortNo" :min="1" /></el-form-item><el-form-item label="预计秒数"><el-input-number v-model="sectionForm.durationSeconds" :min="1" /></el-form-item><el-button type="primary" @click="saveSection">添加章节</el-button></el-form><el-table :data="sections" empty-text="还没有章节"><el-table-column prop="sortNo" label="顺序" width="70" /><el-table-column prop="title" label="章节" /><el-table-column prop="durationSeconds" label="预计秒数" width="110" /><el-table-column label="视频" width="100"><template #default="s"><el-tag :type="s.row.videoAvailable?'success':'info'">{{ s.row.videoAvailable?'已上传':'未上传' }}</el-tag></template></el-table-column><el-table-column label="操作" width="220"><template #default="s"><el-upload :show-file-list="false" accept="video/mp4" :http-request="o=>uploadVideo(s.row,o)"><el-button size="small">上传 MP4</el-button></el-upload><el-button size="small" type="danger" link @click="removeSection(s.row)">删除</el-button></template></el-table-column></el-table></el-dialog>

    <el-dialog v-model="answerDialog" title="回复学生问题" width="520px"><p class="question-preview">{{ answering?.content }}</p><el-input v-model="answerText" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="写下回复内容" /><template #footer><el-button @click="answerDialog=false">取消</el-button><el-button type="primary" @click="sendAnswer">发送回复</el-button></template></el-dialog>

    <el-dialog v-model="quizDialog" title="新建测验题" width="600px"><el-form label-position="top"><el-form-item label="题目"><el-input v-model="quizForm.question" /></el-form-item><el-form-item v-for="key in ['A','B','C','D']" :key="key" :label="`选项${key}`"><el-input v-model="quizForm[`option${key}`]" /></el-form-item><el-form-item label="正确答案"><el-radio-group v-model="quizForm.correctOption"><el-radio v-for="key in ['A','B','C','D']" :key="key" :label="key">{{ key }}</el-radio></el-radio-group></el-form-item><el-form-item label="答案解析"><el-input v-model="quizForm.explanation" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="quizDialog=false">取消</el-button><el-button type="primary" @click="saveQuiz">保存题目</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/local'
import logo from '@/assets/logo.png'
import avatar from '@/assets/icon_touxiang.png'

const checked = ref(false)
const authorized = ref(false)
const profile = ref({})
const active = ref('dashboard')
const loginLoading = ref(false)
const loginForm = ref({ username: 'demo_admin', password: 'Demo@123456' })
const dashboardData = ref({ users: 0, publishedCourses: 0, learners: 0, revenue: 0, pendingRefunds: 0, openQuestions: 0, topCourses: [] })
const courseList = ref([])
const questionList = ref([])
const refundList = ref([])
const userList = ref([])
const userTotal = ref(0)
const userQuery = ref({ keyword: '', page: 1, pageSize: 10 })
const courseDialog = ref(false)
const courseForm = ref({ title: '', category: '后端开发', teacherName: '', price: 199, description: '' })
const sectionDialog = ref(false)
const selectedCourse = ref(null)
const sections = ref([])
const sectionForm = ref({ title: '', sortNo: 1, durationSeconds: 600 })
const answerDialog = ref(false)
const answering = ref(null)
const answerText = ref('')
const quizDialog = ref(false)
const quizCourseId = ref(null)
const quizSectionId = ref(null)
const quizSections = ref([])
const quizList = ref([])
const quizForm = ref({ question: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: 'A', explanation: '' })
const hasToken = computed(() => !!sessionStorage.getItem('yunrui_token'))
const viewTitle = computed(() => ({ dashboard: '数据概览', courses: '课程与视频', questions: '问答与退款', users: '用户管理', quizzes: '章节测验' }[active.value]))

onMounted(bootstrap)

async function bootstrap() {
  try {
    const id = Number(sessionStorage.getItem('yunrui_user_id') || 0)
    if (!id || !hasToken.value) return
    profile.value = await api.profile(id)
    authorized.value = profile.value.role === 'ADMIN'
    if (authorized.value) await loadDashboard()
  } catch (error) {
    ElMessage.error(api.errorMessage(error))
  } finally {
    checked.value = true
  }
}

async function login() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请填写账号和密码')
  loginLoading.value = true
  try {
    const result = await api.login(loginForm.value)
    sessionStorage.setItem('yunrui_token', result.token)
    sessionStorage.setItem('yunrui_user_id', result.userId)
    sessionStorage.setItem('yunrui_nickname', loginForm.value.username)
    await bootstrap()
    if (!authorized.value) ElMessage.warning(`当前登录账号 ${profile.value.username || loginForm.value.username} 不是管理员，请使用 demo_admin`)
    else ElMessage.success('管理员登录成功')
  } catch (error) { ElMessage.error(api.errorMessage(error)) } finally { loginLoading.value = false }
}

function goStudent() { window.location.href = '/index.html' }
function logout() { sessionStorage.removeItem('yunrui_token'); sessionStorage.removeItem('yunrui_user_id'); sessionStorage.removeItem('yunrui_nickname'); authorized.value = false; checked.value = true; ElMessage.success('已退出管理员账号') }
function clearSession() { sessionStorage.removeItem('yunrui_token'); sessionStorage.removeItem('yunrui_user_id'); sessionStorage.removeItem('yunrui_nickname'); profile.value = {}; authorized.value = false; checked.value = true; ElMessage.success('已清除当前账号，请重新登录') }
async function switchView(view) { active.value = view; if (view === 'dashboard') await loadDashboard(); if (view === 'courses') await loadCourses(); if (view === 'questions') await loadServiceData(); if (view === 'users') await loadUsers(); if (view === 'quizzes') { await loadCourses(); if (!quizCourseId.value && courseList.value.length) { quizCourseId.value = courseList.value[0].id; await loadQuizSections() } } }
async function loadDashboard() { try { dashboardData.value = await api.dashboard() } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function loadCourses() { try { courseList.value = await api.adminCourses() } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function loadServiceData() { try { questionList.value = await api.adminQuestions(); refundList.value = await api.adminRefunds() } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function loadUsers() { try { const result = await api.adminUsers(userQuery.value); userList.value = result.records; userTotal.value = result.total } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function saveCourse() { try { await api.createAdminCourse(courseForm.value); courseDialog.value = false; courseForm.value = { title: '', category: '后端开发', teacherName: '', price: 199, description: '' }; await loadCourses(); ElMessage.success('课程草稿已创建') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function changeStatus(course, status) { try { await api.changeCourseStatus(course.id, status); await loadCourses(); ElMessage.success(status === 'PUBLISHED' ? '课程已发布' : '课程已下架') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function openSections(course) { try { selectedCourse.value = course; sections.value = await api.adminSections(course.id); sectionForm.value = { title: '', sortNo: sections.value.length + 1, durationSeconds: 600 }; sectionDialog.value = true } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function saveSection() { if (!sectionForm.value.title.trim()) return ElMessage.warning('请填写章节名称'); try { await api.createAdminSection(selectedCourse.value.id, sectionForm.value); sections.value = await api.adminSections(selectedCourse.value.id); sectionForm.value = { title: '', sortNo: sections.value.length + 1, durationSeconds: 600 }; ElMessage.success('章节已添加') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function uploadVideo(section, options) { try { await api.uploadSectionVideo(section.id, options.file, options.onProgress); sections.value = await api.adminSections(selectedCourse.value.id); options.onSuccess?.(); ElMessage.success('视频上传完成') } catch (error) { options.onError?.(error); ElMessage.error(api.errorMessage(error)) } }
async function removeSection(section) { try { await ElMessageBox.confirm(`确认删除章节“${section.title}”及其视频吗？`, '删除章节'); await api.deleteAdminSection(section.id); sections.value = await api.adminSections(selectedCourse.value.id); ElMessage.success('章节已删除') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(api.errorMessage(error)) } }
function answerQuestion(question) { answering.value = question; answerText.value = ''; answerDialog.value = true }
async function sendAnswer() { if (!answerText.value.trim()) return ElMessage.warning('请填写回复内容'); try { await api.answerQuestion(answering.value.id, answerText.value); answerDialog.value = false; await loadServiceData(); ElMessage.success('回复已发送') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function decideRefund(item, approve) { try { const result = await ElMessageBox.prompt(approve ? '填写通过说明' : '填写拒绝原因', approve ? '通过退款' : '拒绝退款', { inputValue: approve ? '符合退款条件' : '不符合退款条件', inputValidator: value => !!value?.trim() || '请填写处理说明' }); if (approve) await api.approveRefund(item.id, result.value); else await api.rejectRefund(item.id, result.value); await loadServiceData(); ElMessage.success(approve ? '退款已通过' : '退款已拒绝') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(api.errorMessage(error)) } }
async function toggleUserStatus(item) { try { await api.updateAdminUser(item.id, { status: item.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE', role: item.role }); await loadUsers(); ElMessage.success('账号状态已更新') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function toggleUserRole(item) { try { await api.updateAdminUser(item.id, { status: item.status, role: item.role === 'ADMIN' ? 'STUDENT' : 'ADMIN' }); await loadUsers(); ElMessage.success('用户角色已更新') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function loadQuizSections() { try { quizSections.value = await api.adminSections(quizCourseId.value); quizSectionId.value = quizSections.value[0]?.id || null; await loadQuizList() } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function loadQuizList() { try { quizList.value = quizSectionId.value ? await api.adminQuizzes(quizSectionId.value) : [] } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function saveQuiz() { if (!quizForm.value.question.trim()) return ElMessage.warning('请填写题目'); try { await api.createAdminQuiz({ sectionId: quizSectionId.value, ...quizForm.value }); quizDialog.value = false; quizForm.value = { question: '', optionA: '', optionB: '', optionC: '', optionD: '', correctOption: 'A', explanation: '' }; await loadQuizList(); ElMessage.success('测验题已创建') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
async function disableQuiz(item) { try { await api.disableAdminQuiz(item.id); await loadQuizList(); ElMessage.success('测验题已停用') } catch (error) { ElMessage.error(api.errorMessage(error)) } }
</script>

<style scoped>
:global(body) { background: #eef2f6; }
.admin-shell { --navy: #101b31; --blue: #3d7cff; --ink: #14213c; --muted: #7f8da3; min-height: 100vh; display: flex; background: #eef2f6; color: var(--ink); }
.admin-sidebar { width: 246px; min-height: 100vh; display: flex; flex-direction: column; padding: 28px 18px 20px; background: var(--navy); color: #fff; }
.admin-brand { display: flex; align-items: center; gap: 11px; padding: 0 10px 44px; }.admin-brand img { width: 39px; height: 39px; object-fit: contain; filter: brightness(0) invert(1); }.admin-brand b { display: block; font-size: 18px; letter-spacing: .04em; }.admin-brand span { display: block; color: #91a0bb; font-size: 11px; margin-top: 5px; }.side-caption { color: #6d7c99; font-size: 10px; letter-spacing: .2em; padding: 0 12px 12px; }.side-nav { display: flex; flex-direction: column; gap: 6px; }.side-nav button, .side-bottom button { border: 0; background: transparent; color: #9aa9c4; text-align: left; padding: 13px 12px; border-radius: 9px; cursor: pointer; font: inherit; transition: all .2s ease; }.side-nav button span { display: inline-block; width: 29px; color: #52627f; font: 11px Georgia, serif; }.side-nav button:hover { color: #fff; background: rgba(255,255,255,.07); }.side-nav button.active { color: #fff; background: linear-gradient(90deg, rgba(61,124,255,.92), rgba(61,124,255,.42)); box-shadow: 0 9px 20px rgba(0,0,0,.15); }.side-nav button.active span { color: #cfe0ff; }.side-bottom { margin-top: auto; display: flex; flex-direction: column; gap: 5px; border-top: 1px solid rgba(255,255,255,.1); padding-top: 15px; }.side-bottom button:hover { color: #fff; }
.admin-main { flex: 1; min-width: 0; }.admin-header { height: 100px; display: flex; align-items: center; justify-content: space-between; padding: 0 44px; background: rgba(255,255,255,.78); border-bottom: 1px solid #e1e7ef; }.admin-header p { color: #8a98ad; font-size: 12px; margin: 0 0 7px; }.admin-header h1 { font-size: 25px; margin: 0; letter-spacing: -.03em; }.header-account { display: flex; align-items: center; gap: 10px; color: #54637a; font-size: 13px; }.header-account :deep(.el-avatar) { border: 2px solid #dde9ff; }.view-content { padding: 38px 44px 70px; max-width: 1450px; margin: 0 auto; }.eyebrow { color: var(--blue); font-size: 10px; font-weight: 700; letter-spacing: .2em; }.stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 16px; }.stat-card { min-height: 145px; padding: 22px; background: #fff; border: 1px solid #e4eaf1; border-radius: 14px; box-shadow: 0 9px 28px rgba(27,49,82,.05); }.stat-card span { color: #7f8da3; font-size: 13px; }.stat-card strong { display: block; color: var(--ink); font: 34px Georgia, serif; margin: 17px 0 8px; }.stat-card small { color: #a0acba; font-size: 11px; }.stat-primary { background: linear-gradient(135deg, #2557c8, #3d7cff); border-color: transparent; color: #fff; }.stat-primary span, .stat-primary small, .stat-primary strong { color: #fff; }.dashboard-grid { display: grid; grid-template-columns: 1.35fr .65fr; gap: 18px; margin-top: 18px; }.panel { background: #fff; border: 1px solid #e4eaf1; border-radius: 14px; box-shadow: 0 9px 28px rgba(27,49,82,.05); padding: 23px; }.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }.panel-heading h2, .content-toolbar h2 { margin: 7px 0 0; font-size: 20px; }.heading-note, .count-pill { color: #8190a7; font-size: 12px; }.count-pill { padding: 5px 9px; border-radius: 99px; background: #eef3fb; color: var(--blue); }.rank-list { display: flex; flex-direction: column; }.rank-row { display: grid; grid-template-columns: 40px minmax(0,1fr) 120px; align-items: center; gap: 12px; padding: 14px 0; border-bottom: 1px solid #eef1f5; }.rank-row:last-child { border-bottom: 0; }.rank-row b { color: #8796aa; font: 19px Georgia, serif; }.rank-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.rank-row strong { color: #e35c62; text-align: right; font-size: 14px; }.insight-panel { background: #182844; color: #fff; }.insight-panel .eyebrow { color: #91ddcb; }.insight-panel h2 { font-size: 23px; margin: 17px 0 26px; }.todo-row { display: flex; justify-content: space-between; border-top: 1px solid rgba(255,255,255,.15); padding: 16px 0; color: #bdcbe0; }.todo-row b { color: #91ddcb; font: 24px Georgia, serif; }.insight-panel p { color: #92a5c1; font-size: 12px; line-height: 1.7; margin: 17px 0 0; }.content-toolbar { display: flex; align-items: end; justify-content: space-between; margin-bottom: 22px; }.content-toolbar p { color: #8996a9; font-size: 13px; margin: 9px 0 0; }.content-toolbar :deep(.el-button) { border-radius: 9px; }.service-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.user-search { display: flex; max-width: 440px; gap: 10px; margin-bottom: 20px; }.user-search :deep(.el-button) { border-radius: 8px; }.quiz-selects { display: flex; align-items: center; gap: 10px; margin-bottom: 22px; }.quiz-selects :deep(.el-select) { width: 260px; }.question-preview { background: #f4f7fb; padding: 13px; border-radius: 8px; color: #55647a; line-height: 1.7; }.loading-state { display: grid; place-items: center; min-height: 100vh; color: #7f8da3; }.admin-login { width: min(430px, calc(100% - 40px)); margin: 12vh auto; padding: 40px; background: #fff; border: 1px solid #e4eaf1; border-radius: 18px; box-shadow: 0 18px 50px rgba(27,49,82,.1); }.login-mark { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 14px; background: var(--navy); color: #a3e7d3; font: 24px Georgia, serif; }.admin-login h1 { margin: 12px 0 8px; font-size: 28px; }.login-desc { color: #8390a3; font-size: 13px; line-height: 1.7; margin-bottom: 22px; }.login-form { margin-top: 20px; }.login-button { width: 100%; height: 42px; border-radius: 9px; }.back-student { display: block; margin: 20px auto 0; border: 0; background: transparent; color: var(--blue); cursor: pointer; }.clear-session { display: block; margin: 10px auto 0; border: 0; background: transparent; color: #8a98ad; cursor: pointer; font-size: 12px; }
@media (max-width: 1080px) { .admin-sidebar { width: 210px; }.admin-header, .view-content { padding-left: 28px; padding-right: 28px; }.stat-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 760px) { .admin-shell { display: block; }.admin-sidebar { width: 100%; min-height: auto; padding: 16px; }.admin-brand { padding-bottom: 18px; }.side-caption, .side-bottom { display: none; }.side-nav { flex-direction: row; overflow-x: auto; }.side-nav button { white-space: nowrap; }.side-nav button span { display: none; }.admin-header { height: auto; padding: 22px 18px; }.view-content { padding: 24px 16px 50px; }.stat-grid, .dashboard-grid, .service-grid { grid-template-columns: 1fr; }.content-toolbar { align-items: flex-start; gap: 14px; flex-direction: column; }.quiz-selects { align-items: stretch; flex-direction: column; }.quiz-selects :deep(.el-select) { width: 100%; }.panel { padding: 16px; }.rank-row { grid-template-columns: 33px minmax(0,1fr) 90px; } }
</style>
