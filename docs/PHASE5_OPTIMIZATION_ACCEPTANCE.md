# Phase 5 前后端优化验收文档

**项目**: Aetheris RAG 系统
**版本**: Phase 5 优化版本
**日期**: 2026-01-09
**状态**: 待验收

---

## 概述

本文档用于验收 Phase 5 的 4 个优化点：
1. **fallbackResources 渲染**：降级资源列表显示
2. **状态持久化**：问答状态恢复
3. **证据不足提示优化**：改进提示格式
4. **纯 LLM 模式**：添加无检索的纯大模型模式

---

## 修改文件清单

### 前端文件（5 个）

1. ✅ `frontend/src/components/chat/FallbackResourceCard.vue`（新建）
2. ✅ `frontend/src/components/chat/AnswerDisplay.vue`（修改）
3. ✅ `frontend/src/composables/useChat.ts`（修改）
4. ✅ `frontend/src/views/chat/ChatView.vue`（修改）
5. ✅ `frontend/src/services/chat.service.ts`（修改）

### 后端文件（3 个）

1. ✅ `backend/src/main/java/com/aetheris/rag/dto/request/AskRequest.java`（修改）
2. ✅ `backend/src/main/java/com/aetheris/rag/service/impl/RagServiceImpl.java`（修改）
3. ✅ `backend/src/main/java/com/aetheris/rag/service/RagService.java`（修改）

### 编译状态

- ✅ 前端编译成功（无 TypeScript 错误）
- ✅ 后端编译成功（无 Java 编译错误）

---

## 验收检查清单

### 优化点 1：fallbackResources 渲染

#### 功能描述
当 LLM 不可用或证据不足时，后端返回降级资源列表 `fallbackResources`，前端正确渲染这些资源卡片。

#### 测试步骤

**前置条件**：
- 启动后端和前端服务
- 使用默认账户登录（`123` / `1234qwer`）

**测试场景 1.1：证据不足场景**
1. 上传少量测试资源（或清空现有资源）
2. 提问一个数据库中没有答案的问题，例如："什么是量子计算的未来发展趋势？"
3. **预期结果**：
   - 显示橙色边框的 Alert 提示"证据不足"
   - 显示优化后的建议列表（3 条建议）
   - 显示"检索到相关资源 X 个"的统计信息
   - 下方显示"降级资源"标题和资源卡片列表（橙色主题）

**测试场景 1.2：LLM 不可用场景**
1. 临时修改配置使 LLM 调用失败（或断网）
2. 提问任意问题
3. **预期结果**：
   - 显示蓝色 Alert 提示"AI 服务暂时不可用"
   - 下方显示"降级资源"标题和资源卡片列表（橙色主题）

**测试场景 1.3：降级资源卡片交互**
1. 在上述任一场景中，点击降级资源卡片
2. **预期结果**：
   - 跳转到资源详情页 `/resources/{id}`
3. 点击"查看资源"按钮
4. **预期结果**：
   - 跳转到资源详情页

#### 验收标准

- [ ] 降级资源卡片正确显示（左侧橙色边框，淡橙色背景 #fff7e6）
- [ ] 卡片显示资源标题
- [ ] 卡片支持点击跳转
- [ ] "查看资源"按钮正常工作
- [ ] 区分于 citations 的蓝色主题

#### 相关文件

- `frontend/src/components/chat/FallbackResourceCard.vue:1-100`
- `frontend/src/components/chat/AnswerDisplay.vue:96-107`

---

### 优化点 2：状态持久化

#### 功能描述
用户点击引用来源跳转到资源详情页后，返回问答页面时能够恢复上次的问答内容和状态。

#### 测试步骤

**前置条件**：
- 已登录系统
- 已完成至少一次问答

**测试场景 2.1：问答状态保存**
1. 提交问题："什么是 RAG？"（确保有资源可回答）
2. 等待答案生成完成
3. **预期结果**：
   - 控制台输出："问答状态已保存"（无用户提示）
   - sessionStorage 中包含 `aetheris_chat_state` 键

**测试场景 2.2：点击引用后返回**
1. 在问答结果页面，点击任意引用来源的"查看资源"按钮
2. 跳转到资源详情页
3. 点击浏览器返回按钮或导航栏返回问答页面
4. **预期结果**：
   - 页面刷新后，上次的问答内容和答案仍然显示
   - 控制台输出："问答状态已恢复"
   - 引用列表、降级资源列表、响应时间等全部保留

**测试场景 2.3：页面刷新后恢复**
1. 完成一次问答后
2. 按 F5 或 Ctrl+R 刷新页面
3. **预期结果**：
   - 问答内容完全保留（答案、引用、响应时间）

**测试场景 2.4：清空功能**
1. 完成一次问答后
2. 点击"清空"按钮
3. **预期结果**：
   - 显示提示"已清空"
   - 答案、引用列表全部消失
   - sessionStorage 中的 `aetheris_chat_state` 被删除

**测试场景 2.5：状态过期**
1. 完成—次问答
2. 在浏览器开发者工具中修改 sessionStorage 中 `aetheris_chat_state` 的 `timestamp` 为 24 小时前的时间戳
3. 刷新页面
4. **预期结果**：
   - 状态被清除，不显示上次的问答内容

**测试场景 2.6：新问答清除旧状态**
1. 完成第一次问答（问题 A）
2. 提交第二次问答（问题 B）
3. **预期结果**：
   - 只显示问题 B 的答案和引用
   - 问题 A 的状态被完全覆盖

#### 验收标准

- [ ] sessionStorage 正确保存状态（包含答案、引用、降级资源、响应时间、问题）
- [ ] 页面刷新后状态恢复正确
- [ ] 点击引用跳转后返回，状态保留
- [ ] 清空按钮同时清除内存和 sessionStorage
- [ ] 状态过期（24 小时）后自动清除
- [ ] 新问答自动覆盖旧状态
- [ ] 无打扰用户的提示（只在控制台输出日志）

#### 相关文件

- `frontend/src/composables/useChat.ts:183-261`（saveState、restoreState、clearState、clearAnswerAndState）
- `frontend/src/views/chat/ChatView.vue:214-229`（onMounted、onBeforeRouteLeave）

---

### 优化点 3：证据不足提示优化

#### 功能描述
优化证据不足场景的提示格式，使用列表形式展示建议，并添加资源统计信息。

#### 测试步骤

**前置条件**：
- 已登录系统

**测试场景 3.1：证据不足提示展示**
1. 提问一个数据库中资源很少的问题："量子计算的未来发展趋势是什么？"
2. **预期结果**：
   - 显示橙色 Alert 提示框
   - 标题："⚠️ 证据不足"
   - 内容："根据现有资料无法完整回答您的问题，建议："
   - 列表项：
     - ✏️ 尝试更具体的问题描述
     - 🔍 使用不同的关键词重新提问
     - 📚 查阅以下相关资源获取更多信息
   - 统计信息："检索到相关资源 X 个"（如果有检索结果）
   - 数字显示为橙色（#faad14），字号 18px

**测试场景 3.2：样式验证**
1. 检查 Alert 样式
2. **预期结果**：
   - 标题加粗，字号 16px
   - 建议列表带图标，每项 6px 上下间距
   - 图标颜色为橙色（#faad14）
   - 统计标题（"检索到相关资源"）字号 14px，灰色（#666）

#### 验收标准

- [ ] Alert 框正确显示（橙色，type="warning"）
- [ ] 标题显示"⚠️ 证据不足"（带图标）
- [ ] 3 条建议列表正确显示（带图标）
- [ ] 资源统计信息正确显示（如果有检索结果）
- [ ] 样式符合设计规范（颜色、字号、间距）

#### 相关文件

- `frontend/src/components/chat/AnswerDisplay.vue:29-57`
- `frontend/src/components/chat/AnswerDisplay.vue:293-333`（样式）

---

### 优化点 4：纯 LLM 模式

#### 功能描述
添加问答模式开关，允许用户选择使用 RAG 模式（检索+生成）或纯 LLM 模式（直接生成，无检索）。

#### 测试步骤

**前置条件**：
- 已登录系统

**测试场景 4.1：RAG 模式（默认）**
1. 打开问答页面
2. **预期结果**：
   - 问答模式开关默认为"RAG 模式"（开启状态）
   - 显示 TopK 配置项
   - 显示提示文字："📄 基于学习资源检索生成答案，包含引用来源"

**测试场景 4.2：切换到纯 LLM 模式**
1. 点击问答模式开关，切换到"纯 LLM 模式"
2. **预期结果**：
   - TopK 配置项隐藏（因为不需要检索参数）
   - 显示提示文字："🤖 直接调用大模型，不使用检索结果"
   - Tooltip 提示："RAG 模式：基于学习资源生成答案，包含引用来源。纯 LLM 模式：直接调用大模型，无引用来源。"

**测试场景 4.3：纯 LLM 模式问答**
1. 切换到"纯 LLM 模式"
2. 提交问题："什么是 RAG？"
3. 等待答案生成
4. **预期结果**：
   - 显示 AI 回答（无引用来源）
   - 引用来源区域不显示（citations 为空列表）
   - 响应时间比 RAG 模式快（因为跳过了检索步骤）
   - 后端日志显示："纯 LLM 模式：跳过检索，直接调用 LLM"

**测试场景 4.4：RAG 模式问答**
1. 切换到"RAG 模式"
2. 设置 TopK 为 5
3. 提交问题："什么是 RAG？"
4. **预期结果**：
   - 显示 AI 回答
   - 显示引用来源列表（citations 不为空）
   - 后端日志显示："执行问答：userId=XXX, question='XXX', useRag=true, topK=5"

**测试场景 4.5：模式开关禁用状态**
1. 提交问题后（loading 状态）
2. **预期结果**：
   - 问答模式开关被禁用（无法切换）
   - TopK 输入框也被禁用

**测试场景 4.6：后端日志验证**
1. 分别在 RAG 模式和纯 LLM 模式下提问
2. 查看后端日志 `logs/backend.log`
3. **预期结果**：
   - RAG 模式：`执行问答：userId=XXX, question='XXX', useRag=true, topK=5`
   - 纯 LLM 模式：`执行问答：userId=XXX, question='XXX', useRag=false, topK=5`
   - 纯 LLM 模式：`纯 LLM 模式：跳过检索，直接调用 LLM`
   - 完成：`问答完成：userId=XXX, latencyMs=XXXms, useRag=XXX, citationsCount=XXX`

#### 验收标准

- [ ] 问答模式开关正确显示（默认 RAG 模式）
- [ ] Switch 组件交互正常（切换状态、禁用状态）
- [ ] Tooltip 和提示文本正确显示
- [ ] RAG 模式下显示 TopK 配置
- [ ] 纯 LLM 模式下隐藏 TopK 配置
- [ ] RAG 模式下返回 citations
- [ ] 纯 LLM 模式下 citations 为空列表
- [ ] 后端正确识别 useRag 参数
- [ ] 后端日志正确记录模式选择
- [ ] 纯 LLM 模式响应更快（无检索耗时）

#### 相关文件

**前端**：
- `frontend/src/views/chat/ChatView.vue:41-63`（问答模式开关）
- `frontend/src/views/chat/ChatView.vue:65`（TopK 条件显示）
- `frontend/src/services/chat.service.ts:10-14`（AskRequest 接口）

**后端**：
- `backend/src/main/java/com/aetheris/rag/dto/request/AskRequest.java:53-60`（useRag 字段）
- `backend/src/main/java/com/aetheris/rag/service/impl/RagServiceImpl.java:54-144`（ask 方法，RAG/纯 LLM 分支）
- `backend/src/main/java/com/aetheris/rag/service/impl/RagServiceImpl.java:204-230`（buildDirectChatSystemPrompt、buildInsufficientEvidenceAnswer）
- `backend/src/main/java/com/aetheris/rag/service/RagService.java:26-55`（接口文档）

---

## 集成测试场景

### 场景 1：完整问答流程（RAG 模式）

**前置条件**：
- 已上传至少 5 个学习资源（PDF/Markdown）
- 已登录系统

**步骤**：
1. 打开问答页面
2. 确认"问答模式"为"RAG 模式"（默认）
3. 设置 TopK 为 5
4. 提问："什么是 RAG？它的核心组件有哪些？"
5. 等待答案生成完成

**预期结果**：
- ✅ 显示 AI 生成的答案（Markdown 格式）
- ✅ 显示引用来源列表（至少 1 个）
- ✅ 显示响应时间（应 < 5000ms）
- ✅ 答案中包含引用标注，如 [资源标题, 位置]
- ✅ 控制台输出："问答状态已保存"
- ✅ sessionStorage 包含 `aetheris_chat_state`

**验证点**：
- 答案质量（基于检索结果）
- 引用准确性
- 响应时间

---

### 场景 2：证据不足 → 切换模式重试

**前置条件**：
- 已登录系统
- 资源库内容较少

**步骤**：
1. 提问一个数据库中没有答案的问题："量子计算在金融领域的应用前景如何？"
2. 观察"证据不足"提示
3. 点击降级资源卡片，查看资源详情
4. 返回问答页面（状态应保留）
5. 切换到"纯 LLM 模式"
6. 重新提交相同问题

**预期结果**：
- 第 1 次（RAG 模式）：
  - ✅ 显示"证据不足"提示（橙色 Alert）
  - ✅ 显示 3 条建议列表
  - ✅ 显示资源统计信息
  - ✅ 显示降级资源列表（如果有检索结果）
- 第 2 次（纯 LLM 模式）：
  - ✅ 显示 AI 生成的答案（无引用）
  - ✅ 不显示"证据不足"提示
  - ✅ 不显示引用来源列表
  - ✅ 响应时间更快（无检索耗时）

**验证点**：
- 模式切换功能
- 状态持久化
- 降级资源显示

---

### 场景 3：LLM 不可用降级

**前置条件**：
- 已登录系统
- 临时断开 LLM API（或修改 API Key 为无效值）

**步骤**：
1. 修改 `application.yml` 中的 `chatgateway.api-key` 为无效值
2. 重启后端服务
3. 提交任意问题："什么是向量数据库？"
4. **预期结果**：
   - ✅ 显示"AI 服务暂时不可用"提示（蓝色 Alert）
   - ✅ 显示降级资源列表
   - ✅ 降级答案包含资源标题、相似度分数、位置、摘要
5. 恢复 API Key，重启服务
6. 切换到"纯 LLM 模式"
7. 提交相同问题
8. **预期结果**：
   - ✅ 显示错误提示："抱歉，AI 服务暂时不可用，请稍后重试。"
   - ✅ 不显示任何资源列表（因为未执行检索）

**验证点**：
- RAG 模式下的降级处理
- 纯 LLM 模式下的错误处理

---

### 场景 4：状态持久化完整流程

**前置条件**：
- 已登录系统
- 已完成问答

**步骤**：
1. 完成—次问答（问题 A）
2. 按 F5 刷新页面
3. **预期结果**：
   - ✅ 问答内容完全保留
4. 点击引用来源的"查看资源"按钮
5. 跳转到资源详情页
6. 点击浏览器返回按钮
7. **预期结果**：
   - ✅ 问答内容完全保留
8. 提交新问题（问题 B）
9. **预期结果**：
   - ✅ 只显示问题 B 的答案（问题 A 被覆盖）
10. 点击"清空"按钮
11. **预期结果**：
    - ✅ 所有内容清空
    - ✅ sessionStorage 中的 `aetheris_chat_state` 被删除
12. 再次刷新页面
13. **预期结果**：
    - ✅ 不显示任何问答内容（空状态）

**验证点**：
- 状态保存和恢复
- 状态覆盖
- 状态清除

---

## 性能验收标准

### 响应时间

| 场景 | P95 阈值 | 测试方法 |
|------|---------|----------|
| RAG 模式问答 | ≤ 5 秒 | 提问 20 次，计算 95 分位值 |
| 纯 LLM 模式问答 | ≤ 3 秒 | 提问 20 次，计算 95 分位值 |
| 状态恢复 | ≤ 100ms | 刷新页面 10 次，测量恢复耗时 |

### 资源占用

- **sessionStorage 大小**：单次问答状态 ≤ 4MB（超过则跳过存储）
- **内存占用**：前端页面内存占用无明显增长（无内存泄漏）

---

## 代码质量验收

### 前端代码规范

- [ ] 所有注释使用中文
- [ ] 变量和函数命名使用英文
- [ ] 无 TypeScript 类型错误
- [ ] 无 ESLint 警告（可忽略 `vue-tsc` 工具问题）

### 后端代码规范

- [ ] 所有注释使用中文（Javadoc、行内注释、日志）
- [ ] 变量和方法命名使用英文
- [ ] 无 Java 编译错误
- [ ] 无 Maven 警告
- [ ] 使用 Lombok 注解（@Data、@Builder、@RequiredArgsConstructor）
- [ ] 遵循项目架构约束（不使用 Record、MyBatis 注解）

### 向后兼容性

- [ ] `useRag` 默认值为 `true`，旧版本前端仍可正常调用
- [ ] 新增字段不影响现有逻辑
- [ ] API 接口签名未破坏性变更

---

## 已知问题和限制

### 1. fallbackResources 数据不完整

**问题描述**：
后端 `buildFallbackResources` 方法只设置 `id` 和 `title`，缺少 `tags`、`fileType`、`description`、`uploadTime`。

**影响**：
- 前端 FallbackResourceCard 组件只能显示资源标题
- 无法显示文件类型图标（PDF/Markdown）
- 无法显示标签和描述

**临时解决方案**：
- 使用通用的文件图标（FileTextOutlined）
- 只显示标题和"查看资源"按钮

**未来优化**：
- 后端注入 `ResourceService`，查询完整资源信息（见计划文档第 1.3 节）

---

### 2. vue-tsc 工具问题

**问题描述**：
`npm run build` 时 vue-tsc 报错：`Search string not found: "/supportedTSExtensions = .*(?=;)/"`

**影响**：
- 无法使用 `npm run build` 进行构建
- 需要使用 `npx vite build` 直接构建

**临时解决方案**：
- 已安装 terser 依赖
- 使用 `npx vite build` 绕过 vue-tsc 类型检查

**未来优化**：
- 升级 vue-tsc 到最新版本
- 或迁移到 Vite 5 的原生类型检查

---

## 验收结论

### 验收人：________________

### 验收日期：________________

### 验收结果：

- [ ] **通过**：所有功能点验收通过，无阻塞性问题
- [ ] **有条件通过**：部分功能点需优化，但不影响核心功能
- [ ] **不通过**：存在阻塞性问题，需修复后重新验收

### 备注：

_______________________________________________________________
_______________________________________________________________
_______________________________________________________________

---

## 附录：技术细节

### A. 状态持久化实现

**存储方案**：sessionStorage（会话级别，关闭浏览器自动清除）

**存储内容**：
```json
{
  "answerResponse": {...},  // 完整响应对象
  "answer": "...",          // 答案文本
  "citations": [...],       // 引用列表
  "evidenceInsufficient": false,  // 证据不足标志
  "fallbackResources": [...],     // 降级资源列表
  "latencyMs": 1234,        // 响应时间
  "lastQuestion": {...},    // 上次问题（支持重试）
  "timestamp": 1704787200000  // 时间戳（用于过期检查）
}
```

**过期时间**：24 小时

**大小限制**：4MB（超过则跳过存储）

---

### B. 纯 LLM 模式实现

**后端逻辑**：
```java
if (useRag) {
  // RAG 模式：检索 → 生成
  citations = searchService.searchAggregated(question, topK);
  answer = chatGateway.chat(buildSystemPrompt(), buildPrompt(question, citations));
} else {
  // 纯 LLM 模式：直接生成
  answer = chatGateway.chat(buildDirectChatSystemPrompt(), question);
  citations = List.of();  // 空列表
}
```

**Prompt 差异**：
- **RAG 模式**：强调基于检索结果，标注引用来源
- **纯 LLM 模式**：不依赖检索结果，直接回答问题

---

### C. 证据不足判断标准

**触发条件**（满足任一即触发）：
1. 检索结果数量 < 2
2. 平均相似度分数 < 0.5

**处理逻辑**：
- 返回固定的提示文本（`buildInsufficientEvidenceAnswer()`）
- 返回降级资源列表（`buildFallbackResources(citations)`）
- 设置 `evidenceInsufficient = true`

---

## 变更历史

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| 1.0.0 | 2026-01-09 | Claude Code | 初始版本，完成 4 个优化点 |

---

**文档结束**
