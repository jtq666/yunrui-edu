<template>
  <div class="site-shell">
    <header class="topbar">
      <div class="nav container">
        <img class="logo" :src="logo" alt="云睿学堂" />
        <div class="brand"><b>云睿学堂</b><span>职业技能学习平台</span></div>
        <el-menu mode="horizontal" :default-active="active" @select="selectMenu">
          <el-menu-item index="courses">课程中心</el-menu-item>
          <el-menu-item index="cart">购物车</el-menu-item>
          <el-menu-item index="favorites">我的收藏</el-menu-item>
          <el-menu-item index="coupons">优惠券</el-menu-item>
          <el-menu-item index="orders">我的订单</el-menu-item>
          <el-menu-item index="learning">我的课程</el-menu-item>
          <el-menu-item index="ranking">学习排行</el-menu-item>
          <el-menu-item index="notifications">消息通知<el-badge v-if="unreadCount" :value="unreadCount" /></el-menu-item>
        </el-menu>
        <div class="account">
          <template v-if="loggedIn"><el-avatar :src="avatar" /><el-button link @click="profileVisible=true">{{ nickname }}</el-button><el-button v-if="profileForm.role==='ADMIN'" link class="admin-link" @click="$router.push('/admin')">进入后台</el-button><el-button link @click="passwordVisible=true">改密码</el-button><el-button link @click="logout">退出</el-button></template>
          <el-button v-else type="primary" @click="loginVisible=true">登录 / 注册</el-button>
        </div>
      </div>
    </header>

    <section v-if="active==='courses'" class="page">
      <div class="hero">
        <div class="container hero-inner">
          <div class="hero-text">
            <p>学习工作台 / 2026</p>
            <h1>把今天学会，变成明天会用</h1>
            <span>从 Java 后端、高并发到大模型智能体，沿着一条清晰路径持续进阶。</span>
          </div>
          <div class="hero-panel" aria-label="学习路径提示">
            <span class="hero-panel-kicker">LEARNING STUDIO</span>
            <strong>后端工程 → AI 应用 → Agent</strong>
            <div class="hero-panel-line"><i></i><i></i><i></i><em>按阶段推进</em></div>
            <small>课程、进度、问答和测验，都在一个学习空间里。</small>
          </div>
        </div>
      </div>
      <div class="container section">
        <div class="section-title"><div><small>课程中心</small><h2>精选课程</h2></div><el-tag>共 {{ courseTotal }} 门</el-tag></div>
        <div class="focus-strip"><div><span>学习路径</span><strong>从基础到项目，把知识串成能力</strong></div><p>每门课程都配套章节进度、笔记、问答和测验</p></div>
        <div class="search-bar"><el-input v-model="courseQuery.keyword" clearable placeholder="搜索课程名称或介绍" @keyup.enter="searchCourses" /><el-select v-model="courseQuery.category" clearable placeholder="全部分类"><el-option v-for="c in categoryList" :key="c" :label="c" :value="c" /></el-select><el-button type="primary" @click="searchCourses">搜索</el-button></div>
        <div class="course-grid">
          <article class="course-card" v-for="(item,index) in courseList" :key="item.id">
            <div class="course-cover"><img :src="index % 2 ? banner2 : banner1" /><span class="course-index">{{ String(index + 1).padStart(2, '0') }}</span><span class="cover-note">学习路径</span></div>
            <div class="card-body"><div class="course-card-tags"><el-tag size="small">{{ item.category }}</el-tag><el-tag v-if="isOwned(item.id)" size="small" type="success">已购买</el-tag></div><h3>{{ item.title }}</h3><p>{{ item.description }}</p><div class="course-meta"><span>讲师：{{ item.teacherName }}</span><span>{{ item.sections.length }} 节</span></div><div class="card-bottom"><strong>{{ isOwned(item.id) ? '已加入学习' : `￥${item.price}` }}</strong><el-button type="primary" @click="openCourse(item)">{{ isOwned(item.id) ? '继续学习' : '查看并购买' }}</el-button></div></div>
          </article>
        </div>
        <el-pagination class="pager" background layout="prev, pager, next" :total="courseTotal" :page-size="courseQuery.pageSize" v-model:current-page="courseQuery.page" @current-change="loadCourses" />
      </div>
    </section>

    <section v-if="active==='cart'" class="container section"><PageTitle title="购物车" subtitle="先选择课程，再统一决定是否下单" /><RequireLogin v-if="!loggedIn" @login="loginVisible=true" /><el-empty v-else-if="!cartList.length" description="购物车还是空的" /><div v-else class="course-grid"><article class="learning-card" v-for="item in cartList" :key="item.id"><div><el-tag>{{ item.category }}</el-tag><h3>{{ item.title }}</h3><p>讲师：{{ item.teacherName }}　￥{{ item.price }}</p></div><div><el-button @click="removeFromCart(item)">移除</el-button><el-button type="primary" @click="openCourse(item)">查看并购买</el-button></div></article></div></section>

    <section v-if="active==='favorites'" class="container section"><PageTitle title="我的收藏" subtitle="保存感兴趣的课程，稍后继续查看" /><RequireLogin v-if="!loggedIn" @login="loginVisible=true" /><el-empty v-else-if="!favoriteList.length" description="还没有收藏课程" /><div v-else class="course-grid"><article class="learning-card" v-for="item in favoriteList" :key="item.id"><div><el-tag type="warning">已收藏</el-tag><h3>{{ item.title }}</h3><p>{{ item.description }}</p></div><div><el-button @click="removeFavorite(item)">取消收藏</el-button><el-button type="primary" @click="openCourse(item)">查看详情</el-button></div></article></div></section>

    <section v-if="active==='coupons'" class="container section">
      <PageTitle title="课程优惠券" subtitle="领取后可在结算时选择优惠方案，系统默认推荐最优惠组合" />
      <RequireLogin v-if="!loggedIn" @login="loginVisible=true" />
      <div v-else class="coupon-grid">
        <div class="coupon" v-for="c in couponList" :key="c.id"><div class="coupon-price"><b>￥{{ c.discount }}</b><span>{{ c.rule }}</span></div><div><h3>{{ c.name }}</h3><p>有效期内课程通用</p><el-button type="danger" round @click="receive(c)">{{ claimed.includes(c.id)?'已领取':'立即领取' }}</el-button></div></div>
      </div>
    </section>

    <section v-if="active==='orders'" class="container section">
      <PageTitle title="我的订单" subtitle="查看价格、优惠、支付状态，也可以取消未支付订单" />
      <RequireLogin v-if="!loggedIn" @login="loginVisible=true" />
      <el-table v-else :data="orderList" empty-text="暂时没有订单">
        <el-table-column prop="id" label="订单编号" min-width="190" /><el-table-column label="课程"><template #default="s">{{ courseName(s.row.courseId) }}</template></el-table-column>
        <el-table-column label="原价"><template #default="s">￥{{ s.row.originalAmount }}</template></el-table-column><el-table-column label="优惠"><template #default="s">-￥{{ s.row.discountAmount }}</template></el-table-column><el-table-column label="应付"><template #default="s"><b class="price">￥{{ s.row.payableAmount }}</b></template></el-table-column>
        <el-table-column label="状态"><template #default="s"><el-tag :type="statusType(s.row.status)">{{ statusText(s.row.status) }}</el-tag><el-tag v-if="refundFor(s.row.id)" style="margin-left:5px">{{ refundText(refundFor(s.row.id).status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="220"><template #default="s"><template v-if="s.row.status==='PENDING_PAYMENT'"><el-button type="primary" size="small" @click="payOrder(s.row)">模拟支付</el-button><el-button size="small" @click="cancel(s.row)">取消</el-button></template><el-button v-else-if="s.row.status==='PAID'&&!refundFor(s.row.id)" size="small" type="danger" plain @click="openRefund(s.row)">申请退款</el-button><span v-else>—</span></template></el-table-column>
      </el-table>
    </section>

    <section v-if="active==='learning'" class="container section">
      <PageTitle title="我的课程" subtitle="支付成功后，课程会由后台消息自动发放到这里" />
      <RequireLogin v-if="!loggedIn" @login="loginVisible=true" />
      <div v-else class="learning-grid"><el-empty v-if="!entitlements.length" description="还没有已购买课程" />
        <article class="learning-card" v-for="item in entitlements" :key="item.orderId">
          <div class="learning-card-main"><el-tag type="success">学习中</el-tag><h3>{{ courseName(item.courseId) }}</h3><p>来源订单：{{ item.orderId }}</p><div class="learning-progress"><div class="learning-progress-meta"><span>学习进度</span><b>{{ summaryOf(item.courseId).percentage }}%</b></div><el-progress :percentage="summaryOf(item.courseId).percentage" :stroke-width="10" :show-text="false" /><small>已完成 {{ summaryOf(item.courseId).completedSections }} / {{ summaryOf(item.courseId).totalSections }} 个章节</small></div></div>
          <div class="learning-actions"><el-button @click="openCompletion(item.courseId)">完成度</el-button><el-button @click="openInteraction(item.courseId)">问答与测验</el-button><el-button type="primary" @click="openLearning(item.courseId)">继续学习</el-button></div>
        </article>
      </div>
    </section>

    <section v-if="active==='ranking'" class="container section">
      <PageTitle title="本月学习排行" subtitle="学习行为形成积分，按积分实时排序" />
      <RequireLogin v-if="!loggedIn" @login="loginVisible=true" />
      <el-table v-else :data="rankList" empty-text="完成一次课程学习后即可上榜"><el-table-column prop="rank" label="排名" width="100" /><el-table-column prop="userId" label="用户编号" /><el-table-column prop="points" label="积分"><template #default="s"><b class="price">{{ s.row.points }}</b></template></el-table-column></el-table>
    </section>

    <section v-if="active==='notifications'" class="container section"><PageTitle title="消息通知" subtitle="查看课程问答回复和系统消息" /><RequireLogin v-if="!loggedIn" @login="loginVisible=true" /><el-empty v-else-if="!notificationList.length" description="暂无消息" /><div v-else><div class="chapter" v-for="n in notificationList" :key="n.id" @click="markRead(n)"><el-tag :type="n.read?'info':'danger'">{{ n.read?'已读':'未读' }}</el-tag>　<b>{{ n.title }}</b>　{{ n.content }}</div></div></section>

    <section v-if="active==='management'" class="container section"><PageTitle title="课程管理" subtitle="创建课程、维护章节、上传视频并处理学生问答和退款" /><div style="margin-bottom:18px"><el-button type="primary" @click="adminVisible=true">新建课程</el-button><el-button @click="loadAdminCourses">刷新</el-button></div><el-table :data="adminCourseList"><el-table-column prop="title" label="课程" /><el-table-column prop="category" label="分类" /><el-table-column prop="teacherName" label="讲师" /><el-table-column prop="price" label="价格" /><el-table-column prop="status" label="状态" /><el-table-column label="操作" width="280"><template #default="s"><el-button size="small" @click="openSectionAdmin(s.row)">章节与视频</el-button><el-button v-if="s.row.status!=='PUBLISHED'" size="small" type="primary" @click="setCourseStatus(s.row,'PUBLISHED')">发布</el-button><el-button v-else size="small" type="warning" @click="setCourseStatus(s.row,'OFFLINE')">下架</el-button></template></el-table-column></el-table><h2 style="margin:32px 0 16px">待回复问题</h2><el-table :data="adminQuestionList" empty-text="没有待回复问题"><el-table-column prop="content" label="学生问题" /><el-table-column prop="courseId" label="课程编号" width="130" /><el-table-column label="操作" width="120"><template #default="s"><el-button type="primary" size="small" @click="openAnswer(s.row)">回复</el-button></template></el-table-column></el-table><h2 style="margin:32px 0 16px">待审核退款</h2><el-table :data="adminRefundList" empty-text="没有待审核退款"><el-table-column prop="orderId" label="订单编号" /><el-table-column prop="userId" label="用户编号" /><el-table-column prop="reason" label="申请原因" /><el-table-column label="操作" width="170"><template #default="s"><el-button type="success" size="small" @click="decideRefund(s.row,true)">通过</el-button><el-button type="danger" size="small" @click="decideRefund(s.row,false)">拒绝</el-button></template></el-table-column></el-table></section>

    <el-dialog v-model="courseVisible" width="620px" title="课程详情">
      <template v-if="selected"><h2>{{ selected.title }}</h2><p class="detail-desc">{{ selected.description }}</p><el-descriptions :column="2" border><el-descriptions-item label="讲师">{{ selected.teacherName }}</el-descriptions-item><el-descriptions-item label="价格">￥{{ selected.price }}</el-descriptions-item><el-descriptions-item label="课程分类">{{ selected.category }}</el-descriptions-item><el-descriptions-item label="章节数量">{{ selected.sections.length }}</el-descriptions-item></el-descriptions><h3 class="catalog-title">课程目录</h3><div class="chapter" v-for="s in selected.sections" :key="s.id">第{{ s.sortNo }}节　{{ s.title }} <span>{{ Math.round(s.durationSeconds/60) }}分钟</span></div></template>
      <template #footer><el-button v-if="selected?.sections?.length" @click="previewCourse">免费试听第一节</el-button><el-button @click="favoriteSelected">收藏课程</el-button><el-button v-if="!isOwned(selected?.id)" @click="cartSelected">加入购物车</el-button><el-button v-if="isOwned(selected?.id)" type="primary" @click="enterOwnedCourse">进入学习</el-button><el-button v-else type="primary" @click="openSettlement">立即购买</el-button></template>
    </el-dialog>

    <el-dialog v-model="settlementVisible" width="560px" title="确认订单">
      <template v-if="selected">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="课程">{{ selected.title }}</el-descriptions-item>
          <el-descriptions-item label="课程原价">￥{{ selected.price }}</el-descriptions-item>
        </el-descriptions>
        <h3 class="catalog-title">选择优惠方案</h3>
        <el-radio-group v-model="selectedPlanIndex" class="coupon-plan-list">
          <el-radio v-for="(plan,index) in couponPlans" :key="index" :value="index" border>
            <span v-if="plan.coupons.length">{{ plan.coupons.map(c=>c.name).join(' + ') }}，优惠￥{{ plan.discountAmount }}</span>
            <span v-else>不使用优惠券</span>
          </el-radio>
        </el-radio-group>
        <div class="settlement-total">应付金额：<b>￥{{ currentCouponPlan?.payableAmount ?? selected.price }}</b></div>
        <p class="preview-tip">系统已推荐优惠最多的方案，你也可以改选其他方案或不使用优惠券。下单时服务器会再次校验。</p>
      </template>
      <template #footer><el-button @click="settlementVisible=false">取消</el-button><el-button type="primary" :loading="orderSubmitting" @click="buySelected">确认创建订单</el-button></template>
    </el-dialog>

    <el-dialog v-model="previewVisible" width="760px" title="免费试听" @closed="stopPreview"><template v-if="previewSection"><h3>{{ previewSection.title }}</h3><video ref="previewPlayer" class="preview-player" :src="previewUrl" controls preload="metadata"></video><p class="preview-tip">当前为第一节试听内容，购买课程后可以解锁完整章节和学习进度记录。</p></template></el-dialog>

    <el-dialog v-model="profileVisible" width="500px" title="个人资料"><el-form label-position="top"><el-form-item label="用户名"><el-input v-model="profileForm.username" disabled /></el-form-item><el-form-item label="昵称"><el-input v-model="profileForm.nickname" /></el-form-item><el-form-item label="邮箱"><el-input v-model="profileForm.email" /></el-form-item><el-form-item label="个人简介"><el-input v-model="profileForm.bio" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="profileVisible=false">取消</el-button><el-button type="primary" @click="saveProfile">保存</el-button></template></el-dialog>

    <el-dialog v-model="learningVisible" width="760px" title="课程学习与笔记" @closed="stopVideo"><template v-if="learningCourse"><h2>{{ learningCourse.title }}</h2><div v-if="currentSection" class="video-box"><h3>正在学习：{{ currentSection.title }}</h3><video ref="videoPlayer" :src="videoUrl" controls preload="metadata" @loadedmetadata="restoreVideoPosition" @timeupdate="trackVideoProgress" @pause="reportVideoProgress" @ended="reportVideoProgress"></video><p>播放位置会自动保存，下次打开该章节将从上次位置继续。</p></div><div class="chapter" v-for="s in learningCourse.sections" :key="s.id"><b>第{{ s.sortNo }}节　{{ s.title }}</b><span><el-tag v-if="progressOf(s.id)">{{ progressOf(s.id).positionSeconds }}秒</el-tag><el-button size="small" type="primary" :disabled="!s.videoAvailable" @click="playSection(s)">{{ s.videoAvailable?'播放视频':'暂无视频' }}</el-button></span></div><h3 class="catalog-title">我的学习笔记</h3><el-input v-model="noteText" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="记录当前章节的重点和疑问" /><div style="text-align:right;margin:10px 0"><el-button type="primary" @click="saveNote">保存笔记</el-button></div><el-empty v-if="!noteList.length" description="还没有笔记" /><div class="chapter" v-for="n in noteList" :key="n.id">{{ n.content }}<span><el-button link type="danger" @click="removeNote(n)">删除</el-button></span></div><h3 class="catalog-title">课程评价</h3><el-rate v-model="reviewScore" /><el-input v-model="reviewText" type="textarea" :rows="2" maxlength="500" placeholder="说说这门课程对你的帮助" /><div style="text-align:right;margin-top:10px"><el-button @click="submitReview">提交评价</el-button></div><p v-if="reviewSummary.total">平均 {{ reviewSummary.average }} 分，共 {{ reviewSummary.total }} 条评价</p></template></el-dialog>

    <el-dialog v-model="refundVisible" width="470px" title="申请退款"><p>订单：{{ refundOrder?.id }}</p><el-input v-model="refundReason" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请填写退款原因" /><template #footer><el-button @click="refundVisible=false">取消</el-button><el-button type="danger" @click="submitRefund">提交审核</el-button></template></el-dialog>

    <el-dialog v-model="adminVisible" width="520px" title="新建课程"><el-form label-position="top"><el-form-item label="课程名称"><el-input v-model="adminForm.title" /></el-form-item><el-form-item label="分类"><el-input v-model="adminForm.category" /></el-form-item><el-form-item label="讲师"><el-input v-model="adminForm.teacherName" /></el-form-item><el-form-item label="价格"><el-input-number v-model="adminForm.price" :min="0" /></el-form-item><el-form-item label="介绍"><el-input v-model="adminForm.description" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="adminVisible=false">取消</el-button><el-button type="primary" @click="saveAdminCourse">保存草稿</el-button></template></el-dialog>

    <el-dialog v-model="sectionAdminVisible" width="700px" :title="`章节与视频：${adminSelectedCourse?.title||''}`"><el-form inline><el-form-item label="章节名称"><el-input v-model="sectionForm.title" /></el-form-item><el-form-item label="顺序"><el-input-number v-model="sectionForm.sortNo" :min="1" /></el-form-item><el-form-item label="预计秒数"><el-input-number v-model="sectionForm.durationSeconds" :min="1" /></el-form-item><el-button type="primary" @click="saveSection">添加章节</el-button></el-form><el-table :data="adminSectionList"><el-table-column prop="sortNo" label="顺序" width="70" /><el-table-column prop="title" label="章节" /><el-table-column prop="durationSeconds" label="预计秒数" width="100" /><el-table-column label="视频" width="100"><template #default="s"><el-tag :type="s.row.videoAvailable?'success':'info'">{{ s.row.videoAvailable?'已上传':'未上传' }}</el-tag></template></el-table-column><el-table-column label="操作" width="210"><template #default="s"><el-upload :show-file-list="false" accept="video/mp4" :http-request="o=>uploadVideo(s.row,o)"><el-button size="small">上传MP4</el-button></el-upload><el-button size="small" type="danger" link @click="removeSection(s.row)">删除</el-button></template></el-table-column></el-table></el-dialog>

    <el-dialog v-model="interactionVisible" width="720px" title="课程问答与章节测验"><template v-if="interactionCourse"><h2>{{ interactionCourse.title }}</h2><el-select v-model="interactionSectionId" placeholder="选择章节" @change="loadInteractionQuiz"><el-option v-for="s in interactionCourse.sections" :key="s.id" :label="s.title" :value="s.id" /></el-select><h3 class="catalog-title">章节测验</h3><el-empty v-if="!quizList.length" description="当前章节暂无测验" /><div v-for="q in quizList" :key="q.id" class="quiz-card"><b>{{ q.question }}</b><el-radio-group v-model="quizSelections[q.id]"><el-radio v-for="(o,i) in q.options" :key="i" :label="'ABCD'[i]">{{ 'ABCD'[i] }}. {{ o }}</el-radio></el-radio-group><el-button size="small" @click="submitQuizAnswer(q)">提交答案</el-button><p v-if="quizResults[q.id]" :class="quizResults[q.id].correct?'correct':'wrong'">{{ quizResults[q.id].correct?'回答正确':'回答错误，正确答案：'+quizResults[q.id].correctOption }}。{{ quizResults[q.id].explanation }}</p></div><h3 class="catalog-title">向老师提问</h3><el-input v-model="questionText" type="textarea" :rows="2" maxlength="1000" show-word-limit /><div style="text-align:right;margin:8px 0"><el-button type="primary" @click="askCurrentQuestion">提交问题</el-button></div><div class="chapter" v-for="q in questionList" :key="q.id"><b>问：{{ q.content }}</b><p>{{ q.answer?'答：'+q.answer:'等待老师回复' }}</p></div></template></el-dialog>

    <el-dialog v-model="answerVisible" width="520px" title="回复学生问题"><p>{{ answeringQuestion?.content }}</p><el-input v-model="answerText" type="textarea" :rows="4" maxlength="1000" show-word-limit /><template #footer><el-button @click="answerVisible=false">取消</el-button><el-button type="primary" @click="submitAnswer">发送回复</el-button></template></el-dialog>

    <el-dialog v-model="dashboardVisible" width="760px" title="平台数据概览"><div class="dashboard-grid"><div><b>{{ dashboardData.users }}</b><span>注册用户</span></div><div><b>{{ dashboardData.publishedCourses }}</b><span>已发布课程</span></div><div><b>{{ dashboardData.learners }}</b><span>学习用户</span></div><div><b>￥{{ dashboardData.revenue }}</b><span>有效收入</span></div><div><b>{{ dashboardData.pendingRefunds }}</b><span>待审退款</span></div><div><b>{{ dashboardData.openQuestions }}</b><span>待答问题</span></div></div><h3>课程销售排行</h3><el-table :data="dashboardData.topCourses"><el-table-column prop="title" label="课程" /><el-table-column prop="sales" label="销量" /><el-table-column prop="revenue" label="收入" /></el-table></el-dialog>

    <el-dialog v-model="quizAdminVisible" width="760px" title="测验题管理"><el-form inline><el-form-item label="课程"><el-select v-model="quizAdminCourseId" @change="loadQuizAdminSections"><el-option v-for="c in adminCourseList" :key="c.id" :label="c.title" :value="c.id" /></el-select></el-form-item><el-form-item label="章节"><el-select v-model="quizAdminSectionId" @change="loadAdminQuizList"><el-option v-for="s in quizAdminSections" :key="s.id" :label="s.title" :value="s.id" /></el-select></el-form-item></el-form><el-button type="primary" :disabled="!quizAdminSectionId" @click="quizEditVisible=true">新建测验题</el-button><el-table :data="adminQuizList"><el-table-column prop="question" label="题目" /><el-table-column prop="correctOption" label="答案" width="70" /><el-table-column label="状态" width="90"><template #default="s">{{ s.row.active?'启用':'停用' }}</template></el-table-column><el-table-column label="操作" width="100"><template #default="s"><el-button v-if="s.row.active" type="danger" link @click="disableQuiz(s.row)">停用</el-button></template></el-table-column></el-table></el-dialog>

    <el-dialog v-model="quizEditVisible" width="600px" title="新建测验题"><el-form label-position="top"><el-form-item label="题目"><el-input v-model="quizForm.question" /></el-form-item><el-form-item v-for="key in ['A','B','C','D']" :key="key" :label="`选项${key}`"><el-input v-model="quizForm[`option${key}`]" /></el-form-item><el-form-item label="正确答案"><el-radio-group v-model="quizForm.correctOption"><el-radio v-for="k in ['A','B','C','D']" :key="k" :label="k">{{ k }}</el-radio></el-radio-group></el-form-item><el-form-item label="答案解析"><el-input v-model="quizForm.explanation" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="quizEditVisible=false">取消</el-button><el-button type="primary" @click="saveQuiz">保存</el-button></template></el-dialog>

    <el-dialog v-model="userAdminVisible" width="820px" title="用户管理"><div class="search-bar"><el-input v-model="userQuery.keyword" placeholder="搜索用户名或昵称" @keyup.enter="loadAdminUsers" /><span></span><el-button @click="loadAdminUsers">搜索</el-button></div><el-table :data="adminUserList"><el-table-column prop="username" label="用户名" /><el-table-column prop="nickname" label="昵称" /><el-table-column prop="status" label="状态" /><el-table-column prop="role" label="角色" /><el-table-column label="操作" width="220"><template #default="s"><el-button size="small" @click="toggleUserStatus(s.row)">{{ s.row.status==='ACTIVE'?'停用':'启用' }}</el-button><el-button size="small" @click="toggleUserRole(s.row)">{{ s.row.role==='ADMIN'?'设为学生':'设为管理员' }}</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="userQuery.page" :page-size="userQuery.pageSize" :total="userTotal" @current-change="loadAdminUsers" /></el-dialog>

    <el-dialog v-model="completionVisible" width="500px" title="课程完成度"><template v-if="completionData"><el-progress type="circle" :percentage="Math.round(completionData.percentage)" /><p>已完成 {{ completionData.completedSections }} / {{ completionData.totalSections }} 个章节</p><div v-if="completionData.certificate" class="certificate"><h2>云睿学堂结课证明</h2><p>证明编号：{{ completionData.certificate.code }}</p></div><el-alert v-else-if="!completionData.eligible" title="全部章节学习达到90%后可以领取结课证明" type="info" :closable="false" /></template><template #footer><el-button v-if="completionData?.eligible&&!completionData?.certificate" type="primary" @click="claimCertificate">领取结课证明</el-button></template></el-dialog>

    <el-dialog v-model="passwordVisible" width="430px" title="修改密码"><el-form label-position="top"><el-form-item label="原密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item><el-form-item label="新密码（至少8位）"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item></el-form><template #footer><el-button @click="passwordVisible=false">取消</el-button><el-button type="primary" @click="savePassword">确认修改</el-button></template></el-dialog>

    <el-dialog v-model="loginVisible" width="430px" :title="authMode==='login'?'账号登录':'注册新账号'">
      <el-form label-position="top"><el-form-item label="用户名"><el-input v-model="authForm.username" /></el-form-item><el-form-item label="密码"><el-input v-model="authForm.password" type="password" show-password /></el-form-item><el-form-item v-if="authMode==='register'" label="昵称"><el-input v-model="authForm.nickname" /></el-form-item></el-form>
      <div class="switch-auth" @click="authMode=authMode==='login'?'register':'login'">{{ authMode==='login'?'没有账号？去注册':'已有账号？去登录' }}</div>
      <template #footer><el-button type="primary" style="width:100%" :loading="authLoading" @click="submitAuth">{{ authMode==='login'?'登录':'注册并登录' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/local'
import logo from '@/assets/logo.png'; import avatar from '@/assets/icon_touxiang.png'; import banner1 from '@/assets/banner1.jpg'; import banner2 from '@/assets/banner2.jpg'

const PageTitle = defineComponent({props:['title','subtitle'],setup:p=>()=>h('div',{class:'page-title'},[h('h1',p.title),h('p',p.subtitle)])})
const RequireLogin = defineComponent({emits:['login'],setup(_,c){return()=>h('div',{class:'login-hint'},[h('p','登录后即可体验该功能'),h('button',{class:'el-button el-button--primary',onClick:()=>c.emit('login')},'立即登录')])}})
const active=ref('courses'), courseList=ref([]), courseTotal=ref(0), categoryList=ref([]), courseQuery=ref({keyword:'',category:'',page:1,pageSize:4}), selected=ref(null), courseVisible=ref(false), settlementVisible=ref(false), couponPlans=ref([]), selectedPlanIndex=ref(0), orderSubmitting=ref(false), previewVisible=ref(false), loginVisible=ref(false), profileVisible=ref(false), passwordVisible=ref(false), dashboardVisible=ref(false), quizAdminVisible=ref(false), quizEditVisible=ref(false), userAdminVisible=ref(false), completionVisible=ref(false), learningVisible=ref(false), interactionVisible=ref(false), answerVisible=ref(false), refundVisible=ref(false), adminVisible=ref(false), sectionAdminVisible=ref(false), authMode=ref('login'), authLoading=ref(false), orderList=ref([]), refundList=ref([]), adminRefundList=ref([]), entitlements=ref([]), learningSummary=ref({}), rankList=ref([]), notificationList=ref([]), claimed=ref([]), cartList=ref([]), favoriteList=ref([]), profileForm=ref({username:'',nickname:'',email:'',bio:'',role:''}), passwordForm=ref({oldPassword:'',newPassword:''}), dashboardData=ref({users:0,publishedCourses:0,learners:0,revenue:0,pendingRefunds:0,openQuestions:0,topCourses:[]}), adminUserList=ref([]), userTotal=ref(0), userQuery=ref({keyword:'',page:1,pageSize:10}), completionData=ref(null), completionCourseId=ref(null), learningCourse=ref(null), currentSection=ref(null), videoPlayer=ref(null), previewPlayer=ref(null), previewSection=ref(null), previewUrl=ref(''), videoUrl=ref(''), progressList=ref([]), lastReportedSecond=ref(-1), noteList=ref([]), noteText=ref(''), reviewScore=ref(5), reviewText=ref(''), reviewSummary=ref({average:0,total:0,records:[]}), interactionCourse=ref(null), interactionSectionId=ref(null), quizList=ref([]), quizSelections=ref({}), quizResults=ref({}), questionText=ref(''), questionList=ref([]), adminQuestionList=ref([]), answeringQuestion=ref(null), answerText=ref(''), refundOrder=ref(null), refundReason=ref(''), adminCourseList=ref([]), adminForm=ref({title:'',category:'后端开发',teacherName:'',price:199,description:''}), adminSelectedCourse=ref(null), adminSectionList=ref([]), sectionForm=ref({title:'',sortNo:1,durationSeconds:600}), quizAdminCourseId=ref(null), quizAdminSectionId=ref(null), quizAdminSections=ref([]), adminQuizList=ref([]), quizForm=ref({question:'',optionA:'',optionB:'',optionC:'',optionD:'',correctOption:'A',explanation:''})
const authForm=ref({username:'student001',password:'12345678',nickname:'小明'})
const userId=ref(Number(sessionStorage.getItem('yunrui_user_id')||0)), nickname=ref(sessionStorage.getItem('yunrui_nickname')||'同学')
const loggedIn=computed(()=>!!sessionStorage.getItem('yunrui_token')&&userId.value>0)
const unreadCount=computed(()=>notificationList.value.filter(n=>!n.read).length)
const currentCouponPlan=computed(()=>couponPlans.value[selectedPlanIndex.value])
const couponList=[{id:3001,name:'课程通用券',discount:20,rule:'满100可用'},{id:3002,name:'进阶学习券',discount:50,rule:'满200可用'},{id:3003,name:'新人专享券',discount:15,rule:'无门槛，不可叠加'}]

onMounted(async()=>{ categoryList.value=await api.categories(); await loadCourses(); if(loggedIn.value) await loadUserData() })
async function selectMenu(index){if(index==='dashboard'){try{dashboardData.value=await api.dashboard();dashboardVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}return}if(index==='quizAdmin'){quizAdminVisible.value=true;if(!adminCourseList.value.length)await loadAdminCourses();if(!quizAdminCourseId.value&&adminCourseList.value.length){quizAdminCourseId.value=adminCourseList.value[0].id;await loadQuizAdminSections()}return}if(index==='userAdmin'){userAdminVisible.value=true;await loadAdminUsers();return}active.value=index}
async function loadCourses(){try{const r=await api.coursePage(courseQuery.value);courseList.value=r.records;courseTotal.value=r.total}catch(e){ElMessage.error(api.errorMessage(e))}}
function searchCourses(){courseQuery.value.page=1;loadCourses()}
async function loadUserData(){await Promise.all([loadOrders(),loadRefunds(),loadLearning(),loadRanks(),loadCart(),loadFavorites(),loadProfile(),loadNotifications()])}
async function loadOrders(){try{orderList.value=await api.orders(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadRefunds(){try{refundList.value=await api.refunds(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadLearning(){try{const list=await api.myCourses(userId.value);entitlements.value=list;const entries=await Promise.all(list.map(async item=>{try{return [String(item.courseId),await api.completion(userId.value,item.courseId)]}catch{return [String(item.courseId),{percentage:0,completedSections:0,totalSections:0}]}}));learningSummary.value=Object.fromEntries(entries)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadRanks(){try{rankList.value=await api.topRanks(currentSeason())}catch(e){ElMessage.error(api.errorMessage(e))}}
function isOwned(courseId){return !!courseId&&entitlements.value.some(item=>String(item.courseId)===String(courseId))}
function openCourse(item){selected.value=item;courseVisible.value=true}
function enterOwnedCourse(){if(!selected.value)return;courseVisible.value=false;openLearning(selected.value.id)}
function previewCourse(){const section=selected.value?.sections?.[0];if(!section)return;previewSection.value=section;previewUrl.value=`/api/videos/sections/${section.id}/preview`;previewVisible.value=true}
function stopPreview(){if(previewPlayer.value)previewPlayer.value.pause();previewUrl.value='';previewSection.value=null}
async function loadCart(){try{cartList.value=await api.cart(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadFavorites(){try{favoriteList.value=await api.favorites(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadProfile(){try{profileForm.value=await api.profile(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function cartSelected(){if(!ensureLogin())return;try{await api.addCart(userId.value,selected.value.id);await loadCart();ElMessage.success('已加入购物车')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function favoriteSelected(){if(!ensureLogin())return;try{await api.addFavorite(userId.value,selected.value.id);await loadFavorites();ElMessage.success('收藏成功')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function removeFromCart(item){try{await api.removeCart(userId.value,item.id);await loadCart();ElMessage.success('已移出购物车')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function removeFavorite(item){try{await api.removeFavorite(userId.value,item.id);await loadFavorites();ElMessage.success('已取消收藏')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function saveProfile(){try{const r=await api.updateProfile(userId.value,profileForm.value);nickname.value=r.nickname;sessionStorage.setItem('yunrui_nickname',r.nickname);profileVisible.value=false;ElMessage.success('个人资料已保存')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function savePassword(){if(passwordForm.value.newPassword.length<8)return ElMessage.warning('新密码至少8位');try{await api.changePassword(userId.value,passwordForm.value);passwordVisible.value=false;passwordForm.value={oldPassword:'',newPassword:''};ElMessage.success('密码已修改，下次登录请使用新密码')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadAdminUsers(){try{const r=await api.adminUsers(userQuery.value);adminUserList.value=r.records;userTotal.value=r.total}catch(e){ElMessage.error(api.errorMessage(e))}}
async function toggleUserStatus(item){try{await api.updateAdminUser(item.id,{status:item.status==='ACTIVE'?'DISABLED':'ACTIVE',role:item.role});await loadAdminUsers();ElMessage.success('账号状态已更新')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function toggleUserRole(item){try{await api.updateAdminUser(item.id,{status:item.status,role:item.role==='ADMIN'?'STUDENT':'ADMIN'});await loadAdminUsers();ElMessage.success('用户角色已更新')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function openCompletion(courseId){try{completionCourseId.value=courseId;completionData.value=await api.completion(userId.value,courseId);completionVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}}
async function claimCertificate(){try{const certificate=await api.issueCertificate(userId.value,completionCourseId.value);completionData.value.certificate=certificate;ElMessage.success('结课证明已生成')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function openLearning(courseId){try{learningCourse.value=await api.course(courseId);noteList.value=await api.notes(userId.value,courseId);progressList.value=await api.progress(userId.value,courseId);reviewSummary.value=await api.reviews(courseId);currentSection.value=null;videoUrl.value='';noteText.value='';reviewText.value='';learningVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}}
function progressOf(sectionId){return progressList.value.find(p=>String(p.sectionId)===String(sectionId))}
async function playSection(section){try{await reportVideoProgress();const access=await api.videoTicket(userId.value,section.id);currentSection.value=section;lastReportedSecond.value=-1;videoUrl.value=`/api/videos/sections/${section.id}?ticket=${encodeURIComponent(access.ticket)}`;await nextTick();videoPlayer.value?.load()}catch(e){ElMessage.error(api.errorMessage(e))}}
function restoreVideoPosition(){const old=progressOf(currentSection.value?.id);if(old&&videoPlayer.value)videoPlayer.value.currentTime=Math.min(old.positionSeconds,Math.max(videoPlayer.value.duration-1,0))}
function trackVideoProgress(){const second=Math.floor(videoPlayer.value?.currentTime||0);if(second>0&&(lastReportedSecond.value<0||second-lastReportedSecond.value>=15)){reportVideoProgress()}}
async function reportVideoProgress(){const player=videoPlayer.value,section=currentSection.value;if(!player||!section||!Number.isFinite(player.duration)||player.duration<=0)return;const position=Math.min(Math.floor(player.currentTime),Math.floor(player.duration));if(position===lastReportedSecond.value)return;lastReportedSecond.value=position;try{await api.reportProgress({userId:userId.value,courseId:learningCourse.value.id,sectionId:section.id,positionSeconds:position,durationSeconds:Math.ceil(player.duration)});const old=progressOf(section.id);if(old){old.positionSeconds=position;old.durationSeconds=Math.ceil(player.duration)}else progressList.value.push({sectionId:section.id,positionSeconds:position,durationSeconds:Math.ceil(player.duration)})}catch(e){ElMessage.error(api.errorMessage(e))}}
function stopVideo(){reportVideoProgress();if(videoPlayer.value)videoPlayer.value.pause();currentSection.value=null;videoUrl.value=''}
async function learnSection(section){try{const seq=Date.now();await api.reportProgress({userId:userId.value,courseId:learningCourse.value.id,sectionId:section.id,positionSeconds:Math.round(section.durationSeconds/2),durationSeconds:section.durationSeconds});await api.addPoints({eventId:`learn-${userId.value}-${learningCourse.value.id}-${section.id}-${seq}`,userId:userId.value,points:30,reason:'完成一次课程学习'});ElMessage.success(`《${section.title}》已学习到50%，获得30积分`);setTimeout(loadRanks,2200)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function saveNote(){if(!noteText.value.trim())return ElMessage.warning('请先填写笔记');const section=currentSection.value||learningCourse.value.sections[0];try{await api.createNote(userId.value,{courseId:learningCourse.value.id,sectionId:section.id,content:noteText.value,videoSecond:Math.floor(videoPlayer.value?.currentTime||0)});noteList.value=await api.notes(userId.value,learningCourse.value.id);noteText.value='';ElMessage.success('笔记已保存，并记录当前播放位置')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function removeNote(note){try{await api.deleteNote(userId.value,note.id);noteList.value=noteList.value.filter(n=>n.id!==note.id);ElMessage.success('笔记已删除')}catch(e){ElMessage.error(api.errorMessage(e))}}
function openRefund(order){refundOrder.value=order;refundReason.value='课程内容与预期不符';refundVisible.value=true}
async function submitRefund(){if(!refundReason.value.trim())return ElMessage.warning('请填写退款原因');try{await api.applyRefund(userId.value,{orderId:refundOrder.value.id,reason:refundReason.value});refundVisible.value=false;await loadRefunds();ElMessage.success('退款申请已提交，请等待管理员审核')}catch(e){ElMessage.error(api.errorMessage(e))}}
function refundFor(orderId){return refundList.value.find(r=>String(r.orderId)===String(orderId))}
function refundText(status){return({PENDING:'退款审核中',APPROVED:'退款已通过',REJECTED:'退款被拒绝'})[status]||status}
async function submitReview(){if(!reviewText.value.trim())return ElMessage.warning('请填写评价内容');try{reviewSummary.value=await api.createReview(learningCourse.value.id,{userId:userId.value,score:reviewScore.value,content:reviewText.value});reviewText.value='';ElMessage.success('评价已提交')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadAdminCourses(){try{adminCourseList.value=await api.adminCourses()}catch(e){ElMessage.error(api.errorMessage(e))}}
async function saveAdminCourse(){try{await api.createAdminCourse(adminForm.value);adminVisible.value=false;adminForm.value={title:'',category:'后端开发',teacherName:'',price:199,description:''};await loadAdminCourses();ElMessage.success('课程草稿已创建')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function setCourseStatus(course,value){try{await api.changeCourseStatus(course.id,value);await Promise.all([loadAdminCourses(),loadCourses()]);ElMessage.success(value==='PUBLISHED'?'课程已发布':'课程已下架')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function openSectionAdmin(course){try{adminSelectedCourse.value=course;adminSectionList.value=await api.adminSections(course.id);sectionForm.value={title:'',sortNo:adminSectionList.value.length+1,durationSeconds:600};sectionAdminVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}}
async function saveSection(){if(!sectionForm.value.title.trim())return ElMessage.warning('请填写章节名称');try{await api.createAdminSection(adminSelectedCourse.value.id,sectionForm.value);adminSectionList.value=await api.adminSections(adminSelectedCourse.value.id);sectionForm.value={title:'',sortNo:adminSectionList.value.length+1,durationSeconds:600};ElMessage.success('章节已添加')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function uploadVideo(section,options){try{await api.uploadSectionVideo(section.id,options.file,options.onProgress);adminSectionList.value=await api.adminSections(adminSelectedCourse.value.id);options.onSuccess?.();ElMessage.success('视频上传完成，购买后即可播放')}catch(e){options.onError?.(e);ElMessage.error(api.errorMessage(e))}}
async function removeSection(section){try{await ElMessageBox.confirm(`确认删除章节“${section.title}”及其上传视频吗？`,'删除章节');await api.deleteAdminSection(section.id);adminSectionList.value=await api.adminSections(adminSelectedCourse.value.id);ElMessage.success('章节已删除')}catch(e){if(e!=='cancel')ElMessage.error(api.errorMessage(e))}}
async function loadQuizAdminSections(){try{quizAdminSections.value=await api.adminSections(quizAdminCourseId.value);quizAdminSectionId.value=quizAdminSections.value[0]?.id||null;await loadAdminQuizList()}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadAdminQuizList(){try{adminQuizList.value=quizAdminSectionId.value?await api.adminQuizzes(quizAdminSectionId.value):[]}catch(e){ElMessage.error(api.errorMessage(e))}}
async function saveQuiz(){if(!quizForm.value.question.trim())return ElMessage.warning('请填写题目');try{await api.createAdminQuiz({sectionId:quizAdminSectionId.value,...quizForm.value});quizEditVisible.value=false;quizForm.value={question:'',optionA:'',optionB:'',optionC:'',optionD:'',correctOption:'A',explanation:''};await loadAdminQuizList();ElMessage.success('测验题已创建')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function disableQuiz(item){try{await api.disableAdminQuiz(item.id);await loadAdminQuizList();ElMessage.success('测验题已停用，历史答题记录仍保留')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function openInteraction(courseId){try{interactionCourse.value=await api.course(courseId);interactionSectionId.value=interactionCourse.value.sections[0]?.id||null;questionList.value=await api.myQuestions(userId.value,courseId);quizSelections.value={};quizResults.value={};questionText.value='';if(interactionSectionId.value)await loadInteractionQuiz();interactionVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadInteractionQuiz(){try{quizList.value=interactionSectionId.value?await api.quizzes(userId.value,interactionSectionId.value):[]}catch(e){ElMessage.error(api.errorMessage(e))}}
async function submitQuizAnswer(question){const selected=quizSelections.value[question.id];if(!selected)return ElMessage.warning('请先选择答案');try{quizResults.value[question.id]=await api.submitQuiz(userId.value,question.id,selected)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function askCurrentQuestion(){if(!questionText.value.trim())return ElMessage.warning('请填写问题');try{await api.askQuestion(userId.value,{courseId:interactionCourse.value.id,sectionId:interactionSectionId.value,content:questionText.value});questionList.value=await api.myQuestions(userId.value,interactionCourse.value.id);questionText.value='';ElMessage.success('问题已提交，老师回复后会发送站内通知')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadAdminQuestions(){try{adminQuestionList.value=await api.adminQuestions()}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadAdminRefunds(){try{adminRefundList.value=await api.adminRefunds()}catch(e){ElMessage.error(api.errorMessage(e))}}
async function decideRefund(item,approve){try{const result=await ElMessageBox.prompt(approve?'填写通过说明':'填写拒绝原因',approve?'通过退款':'拒绝退款',{inputValue:approve?'符合退款条件':'不符合退款条件',inputValidator:v=>!!v?.trim()||'请填写处理说明'});if(approve)await api.approveRefund(item.id,result.value);else await api.rejectRefund(item.id,result.value);await loadAdminRefunds();ElMessage.success(approve?'退款已通过并收回课程权限':'退款已拒绝')}catch(e){if(e!=='cancel'&&e!=='close')ElMessage.error(api.errorMessage(e))}}
function openAnswer(question){answeringQuestion.value=question;answerText.value='';answerVisible.value=true}
async function submitAnswer(){if(!answerText.value.trim())return ElMessage.warning('请填写回复');try{await api.answerQuestion(answeringQuestion.value.id,answerText.value);answerVisible.value=false;await loadAdminQuestions();ElMessage.success('回复已发送，学生会收到通知')}catch(e){ElMessage.error(api.errorMessage(e))}}
async function loadNotifications(){try{notificationList.value=await api.notifications(userId.value)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function markRead(item){if(item.read)return;try{await api.readNotification(userId.value,item.id);item.read=true}catch(e){ElMessage.error(api.errorMessage(e))}}
function ensureLogin(){if(!loggedIn.value){loginVisible.value=true;return false}return true}
async function submitAuth(){if(!authForm.value.username||!authForm.value.password)return ElMessage.warning('请填写用户名和密码');authLoading.value=true;try{const r=authMode.value==='login'?await api.login(authForm.value):await api.register(authForm.value);sessionStorage.setItem('yunrui_token',r.token);sessionStorage.setItem('yunrui_user_id',r.userId);sessionStorage.setItem('yunrui_nickname',authForm.value.nickname||authForm.value.username);userId.value=r.userId;nickname.value=authForm.value.nickname||authForm.value.username;loginVisible.value=false;ElMessage.success(authMode.value==='login'?'登录成功':'注册成功，已经自动登录');await loadUserData()}catch(e){ElMessage.error(api.errorMessage(e))}finally{authLoading.value=false}}
function logout(){sessionStorage.removeItem('yunrui_token');sessionStorage.removeItem('yunrui_user_id');sessionStorage.removeItem('yunrui_nickname');userId.value=0;nickname.value='同学';orderList.value=[];entitlements.value=[];learningSummary.value={};rankList.value=[];cartList.value=[];favoriteList.value=[];ElMessage.success('已退出登录')}
async function receive(c){if(!ensureLogin())return;if(claimed.value.includes(c.id))return;try{await api.claimCoupon(userId.value,c.id);claimed.value.push(c.id);ElMessage.success(`已领取${c.name}`)}catch(e){ElMessage.error(api.errorMessage(e))}}
async function openSettlement(){if(!ensureLogin())return;try{couponPlans.value=await api.couponPlans(userId.value,selected.value.id);selectedPlanIndex.value=0;courseVisible.value=false;settlementVisible.value=true}catch(e){ElMessage.error(api.errorMessage(e))}}
async function buySelected(){if(!ensureLogin()||!currentCouponPlan.value)return;orderSubmitting.value=true;try{const couponIds=currentCouponPlan.value.coupons.map(c=>c.id);await api.createOrder(userId.value,selected.value.id,couponIds);settlementVisible.value=false;active.value='orders';await loadOrders();ElMessage.success('订单创建成功')}catch(e){ElMessage.error(api.errorMessage(e))}finally{orderSubmitting.value=false}}
async function payOrder(row){try{await ElMessageBox.confirm(`本次模拟支付￥${row.payableAmount}，不接入真实支付平台。`,'模拟支付');await api.pay(row.id,row.payableAmount);ElMessage.success('支付成功，后台正在发放课程');await new Promise(r=>setTimeout(r,1500));await Promise.all([loadOrders(),loadLearning()])}catch(e){if(e!=='cancel')ElMessage.error(api.errorMessage(e))}}
async function cancel(row){try{await api.cancelOrder(userId.value,row.id);ElMessage.success('订单已取消，优惠券已释放');await loadOrders()}catch(e){ElMessage.error(api.errorMessage(e))}}
async function learn(courseId){const c=courseList.value.find(x=>x.id===courseId),s=c?.sections?.[0];if(!s)return;try{const seq=Date.now();await api.reportProgress({userId:userId.value,courseId,sectionId:s.id,positionSeconds:Math.round(s.durationSeconds/2),durationSeconds:s.durationSeconds});await api.addPoints({eventId:`learn-${userId.value}-${courseId}-${seq}`,userId:userId.value,points:30,reason:'完成一次课程学习'});ElMessage.success(`已学习《${s.title}》到50%，获得30积分`);setTimeout(loadRanks,2200)}catch(e){ElMessage.error(api.errorMessage(e))}}
function summaryOf(courseId){return learningSummary.value[String(courseId)]||{percentage:0,completedSections:0,totalSections:0}}
function courseName(id){return courseList.value.find(c=>String(c.id)===String(id))?.title||`课程${id}`}
function statusText(s){return({PENDING_PAYMENT:'等待支付',PAID:'已支付',CLOSED:'已关闭',REFUNDED:'已退款'})[s]||s}
function statusType(s){return({PENDING_PAYMENT:'warning',PAID:'success',CLOSED:'info',REFUNDED:'danger'})[s]||''}
function currentSeason(){const d=new Date();return `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}`}
</script>

<style scoped>
:global(body) { background: #f4f6f8; }
:global(:focus-visible) { outline: 3px solid rgba(47, 109, 246, .45); outline-offset: 3px; }

.site-shell {
  --ink: #10203d;
  --ink-soft: #52637d;
  --blue: #2f6df6;
  --mint: #9de6d2;
  --line: #e6ebf1;
  min-height: 100vh;
  background: #f4f6f8;
  color: var(--ink);
}

.topbar {
  height: 76px;
  background: rgba(255, 255, 255, .92);
  border-bottom: 1px solid var(--line);
  box-shadow: 0 8px 24px rgba(16, 32, 61, .04);
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(16px);
}

.nav { height: 76px; display: flex; align-items: center; gap: 12px; }
.logo { width: 48px; height: 48px; object-fit: contain; }
.brand { display: flex; flex-direction: column; min-width: 154px; line-height: 1.2; }
.brand b { font-size: 19px; letter-spacing: .04em; }
.brand span { color: #8996aa; font-size: 11px; margin-top: 5px; }
.nav .el-menu { flex: 1; border: none; background: transparent; }
.nav :deep(.el-menu-item) { color: #61718b; font-size: 13px; height: 76px; padding: 0 13px; }
.nav :deep(.el-menu-item:hover), .nav :deep(.el-menu-item.is-active) { color: var(--blue); background: transparent; }
.nav :deep(.el-menu-item.is-active::after) { height: 3px; border-radius: 3px 3px 0 0; }
.account { display: flex; align-items: center; gap: 7px; white-space: nowrap; }
.account :deep(.el-button.is-link) { color: #61718b; padding: 4px; }
.account :deep(.el-button.is-link:hover) { color: var(--blue); }
.account :deep(.el-avatar) { border: 2px solid #e6f1ff; }
.coupon-plan-list { display: grid; gap: 10px; align-items: stretch; }
.coupon-plan-list :deep(.el-radio) { margin-right: 0; height: auto; min-height: 44px; padding: 10px 14px; }
.settlement-total { margin-top: 18px; text-align: right; font-size: 16px; }
.settlement-total b { color: #e34d59; font-size: 24px; }

.page { min-height: calc(100vh - 76px); }
.hero {
  min-height: 348px;
  background: linear-gradient(106deg, rgba(10, 25, 54, .96) 0%, rgba(16, 43, 91, .86) 54%, rgba(34, 95, 169, .54) 100%), url('@/assets/banner3.jpg') center / cover;
  position: relative;
  overflow: hidden;
}
.hero::after { content: ''; position: absolute; width: 430px; height: 430px; right: 8%; top: -210px; border: 1px solid rgba(157, 230, 210, .28); border-radius: 50%; box-shadow: 0 0 0 34px rgba(157, 230, 210, .05), 0 0 0 70px rgba(157, 230, 210, .035); }
.hero-inner { min-height: 348px; display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 1; }
.hero-text { max-width: 660px; color: #fff; padding: 48px 0; }
.hero-text p { color: var(--mint); font-weight: 700; font-size: 12px; letter-spacing: .2em; text-transform: uppercase; }
.hero h1 { font-size: clamp(32px, 4vw, 50px); line-height: 1.14; letter-spacing: -.04em; margin: 15px 0 18px; }
.hero-text > span { display: block; color: #d7e2f1; font-size: 16px; line-height: 1.8; max-width: 560px; }
.hero-panel { width: 298px; padding: 24px; border: 1px solid rgba(255,255,255,.2); border-radius: 18px; background: rgba(255,255,255,.1); color: #fff; box-shadow: 0 20px 50px rgba(5, 18, 42, .18); backdrop-filter: blur(12px); }
.hero-panel-kicker { color: var(--mint); font-size: 10px; letter-spacing: .2em; }
.hero-panel strong { display: block; font-size: 19px; line-height: 1.5; margin: 17px 0 20px; }
.hero-panel-line { display: flex; align-items: center; gap: 7px; margin-bottom: 15px; }
.hero-panel-line i { width: 42px; height: 4px; border-radius: 9px; background: var(--mint); opacity: .95; }
.hero-panel-line i:nth-child(2) { opacity: .65; }.hero-panel-line i:nth-child(3) { opacity: .32; }
.hero-panel-line em { color: #d7e2f1; font-size: 11px; font-style: normal; margin-left: 4px; }
.hero-panel small { color: #c1d0e3; line-height: 1.65; }

.section { padding: 45px 0 78px; }
.section-title, .page-title { display: flex; align-items: end; justify-content: space-between; margin-bottom: 24px; }
.section-title small { color: var(--blue); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
.section-title h2, .page-title h1 { color: var(--ink); font-size: 30px; line-height: 1.2; letter-spacing: -.03em; margin-top: 8px; }
.section-title :deep(.el-tag) { border: none; color: #46617d; background: #e7eef8; padding: 0 12px; }
.page-title { display: block; }.page-title p { color: #8693a6; margin-top: 8px; }
.focus-strip { display: flex; align-items: center; justify-content: space-between; padding: 15px 18px; background: #eaf6f3; border: 1px solid #d7eee8; border-radius: 13px; margin-bottom: 20px; }
.focus-strip div { display: flex; align-items: baseline; gap: 12px; }.focus-strip span { color: #4e9b88; font-size: 12px; font-weight: 700; }.focus-strip strong { color: #173c4b; font-size: 14px; }.focus-strip p { color: #6d8d91; font-size: 12px; margin: 0; }
.search-bar { display: grid; grid-template-columns: 1fr 220px 100px; gap: 12px; margin-bottom: 25px; }
.search-bar :deep(.el-input__wrapper), .search-bar :deep(.el-select .el-input__wrapper) { border-radius: 11px; box-shadow: 0 0 0 1px #e1e7ef inset; }
.search-bar :deep(.el-button) { border-radius: 11px; }

.course-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }
.course-card { background: #fff; border: 1px solid #e9edf2; border-radius: 17px; overflow: hidden; box-shadow: 0 11px 34px rgba(31, 56, 88, .07); display: grid; grid-template-columns: 205px minmax(0, 1fr); transition: transform .2s ease, box-shadow .2s ease; }
.course-card:hover { transform: translateY(-4px); box-shadow: 0 17px 42px rgba(31, 56, 88, .13); }
.course-cover { min-height: 238px; position: relative; overflow: hidden; background: #182c4a; }
.course-cover::after { content: ''; position: absolute; inset: 0; background: linear-gradient(160deg, transparent 35%, rgba(10, 25, 54, .72)); pointer-events: none; }
.course-cover img { width: 100%; height: 100%; min-height: 238px; object-fit: cover; display: block; transition: transform .4s ease; }
.course-card:hover .course-cover img { transform: scale(1.04); }
.course-index { position: absolute; left: 16px; bottom: 14px; z-index: 1; color: #fff; font-family: Georgia, serif; font-size: 28px; letter-spacing: .05em; }
.cover-note { position: absolute; right: 14px; top: 14px; z-index: 1; color: #fff; font-size: 10px; letter-spacing: .12em; writing-mode: vertical-rl; opacity: .85; }
.card-body { padding: 22px 22px 19px; min-width: 0; display: flex; flex-direction: column; }
.course-card-tags { display: flex; gap: 7px; align-items: center; min-height: 24px; }.course-card-tags :deep(.el-tag) { border: none; border-radius: 6px; }
.card-body h3 { color: var(--ink); font-size: 19px; line-height: 1.4; margin: 13px 0 8px; }
.card-body p { color: #758399; font-size: 13px; line-height: 1.65; height: 43px; overflow: hidden; margin: 0; }
.course-meta { display: flex; justify-content: space-between; gap: 10px; color: #93a0b0; font-size: 12px; margin: 14px 0 18px; }
.card-bottom { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: auto; }.card-bottom strong, .price { color: #e24f5a; font-size: 20px; letter-spacing: -.02em; }.card-bottom :deep(.el-button) { border-radius: 9px; }

.coupon-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }.coupon { display: flex; background: #fff; border-radius: 15px; padding: 22px; gap: 18px; border: 1px solid #e9edf2; border-top: 4px solid #ef6d72; box-shadow: 0 8px 25px rgba(31, 56, 88, .06); }.coupon-price { padding-right: 18px; border-right: 1px dashed #dbe1e8; display: flex; flex-direction: column; }.coupon-price b { color: #e2555d; font-size: 28px; }.coupon-price span, .coupon p { color: #8a96a7; font-size: 12px; margin: 7px 0 13px; }
.learning-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }.learning-card { display: flex; justify-content: space-between; align-items: center; gap: 22px; background: #fff; padding: 24px; border: 1px solid #e9edf2; border-radius: 15px; box-shadow: 0 8px 25px rgba(31, 56, 88, .06); }.learning-card h3 { margin: 12px 0; }.learning-card p { color: #8a93a3; font-size: 13px; }.learning-card-main { flex: 1; min-width: 0; }.learning-progress { margin-top: 18px; }.learning-progress-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 7px; color: #7b8492; font-size: 13px; }.learning-progress-meta b { color: var(--blue); font-size: 16px; }.learning-progress small { display: block; color: #9aa3b1; margin-top: 8px; }.learning-actions { display: flex; flex-direction: column; gap: 8px; min-width: 92px; }.learning-actions .el-button { margin-left: 0; border-radius: 9px; }
.login-hint { text-align: center; background: #fff; padding: 80px; border: 1px solid #e9edf2; border-radius: 15px; box-shadow: 0 8px 25px rgba(31, 56, 88, .05); }.login-hint p { color: #7b8492; margin-bottom: 18px; }
.detail-desc { color: #6f7988; line-height: 1.8; margin: 12px 0 20px; }.catalog-title { margin: 22px 0 10px; }.chapter { padding: 13px 15px; background: #f5f7fa; margin-bottom: 8px; border-radius: 8px; }.chapter span { float: right; color: #8993a2; }.switch-auth { color: var(--blue); cursor: pointer; }
.container { width: min(1180px, calc(100% - 40px)); margin: 0 auto; }
.pager { justify-content: center; margin-top: 30px; }.video-box { background: #0e182a; color: #fff; padding: 16px; border-radius: 13px; margin: 16px 0; box-shadow: 0 12px 30px rgba(10, 25, 54, .16); }.video-box video { width: 100%; max-height: 380px; background: #000; border-radius: 8px; }.video-box p { color: #aeb9c8; font-size: 13px; margin-top: 8px; }.preview-player { width: 100%; max-height: 420px; background: #101722; border-radius: 10px; }.preview-tip { color: #7b8492; font-size: 13px; margin: 12px 0 0; }
.quiz-card { padding: 16px; background: #f6f8fb; border-radius: 9px; margin: 12px 0; }.quiz-card .el-radio-group { display: flex; flex-direction: column; align-items: flex-start; margin: 10px 0; }.correct { color: #28a66a; }.wrong { color: #e24b4b; }.dashboard-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 24px; }.dashboard-grid > div { background: #f4f7fd; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; }.dashboard-grid b { color: var(--blue); font-size: 25px; }.dashboard-grid span { color: #7d8796; margin-top: 6px; }

@media (max-width: 1050px) { .nav :deep(.el-menu-item) { padding: 0 8px; }.brand { min-width: 128px; }.account :deep(.el-button.is-link:nth-last-child(-n+2)) { display: none; } }
@media (max-width: 900px) { .container { width: auto; margin: 0 16px; }.topbar { height: auto; }.nav { height: auto; min-height: 76px; flex-wrap: wrap; padding: 10px 0; }.nav .el-menu { order: 3; width: 100%; overflow-x: auto; }.nav :deep(.el-menu-item) { height: 42px; }.account { margin-left: auto; }.hero { min-height: 430px; }.hero-inner { min-height: 430px; display: block; padding-top: 20px; }.hero-text { padding: 42px 0 26px; }.hero-panel { width: min(100%, 330px); }.hero h1 { font-size: 34px; }.hero-text > span { font-size: 14px; }.focus-strip { display: block; }.focus-strip p { margin-top: 8px; }.course-grid, .learning-grid, .coupon-grid { grid-template-columns: 1fr; }.course-card { grid-template-columns: 155px minmax(0, 1fr); }.course-cover, .course-cover img { min-height: 220px; }.course-meta { display: block; line-height: 1.8; }.learning-card { align-items: flex-start; flex-direction: column; }.learning-actions { flex-direction: row; flex-wrap: wrap; width: 100%; min-width: 0; }.search-bar { grid-template-columns: 1fr; } }
@media (max-width: 580px) { .brand span { display: none; }.brand b { font-size: 16px; }.logo { width: 40px; height: 40px; }.hero h1 { font-size: 30px; }.section { padding-top: 32px; }.section-title h2, .page-title h1 { font-size: 25px; }.course-card { display: block; }.course-cover, .course-cover img { height: 166px; min-height: 166px; }.course-cover img { width: 100%; }.card-body { padding: 18px; }.account :deep(.el-avatar) { display: none; }.account :deep(.el-button) { font-size: 12px; }.section-title { align-items: flex-start; }.focus-strip strong { font-size: 13px; } }
@media (prefers-reduced-motion: reduce) { .course-card, .course-cover img { transition: none; } }
</style>
