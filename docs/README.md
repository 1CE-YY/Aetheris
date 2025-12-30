# Aetheris 项目文档

此目录包含 Aetheris RAG 系统的完整文档。

---

## 📚 文档导航

### 快速开始
- [启动指南](STARTUP_GUIDE.md) - 一键启动脚本和完整启动步骤

### 验收文档
- [Phase 1-2 验收报告](PHASE1_2_ACCEPTANCE_REPORT.md) - 详细验收结果（97.1% 通过）
- [Phase 1-2 验收清单](PHASE1_2_ACCEPTANCE_CHECKLIST.md) - 35 项验收检查清单

### 开发日志
- [开发日志](dev-logs/development-log.md) - 问题修复记录和技术决策

---

## 📖 文档分类

### 按用途分类

#### 🚀 新手入门
1. [STARTUP_GUIDE.md](STARTUP_GUIDE.md)
   - 一键启动脚本使用
   - 完整的环境配置和启动步骤
   - 快速验证安装

#### 📋 项目验收
1. [PHASE1_2_ACCEPTANCE_REPORT.md](PHASE1_2_ACCEPTANCE_REPORT.md)
   - 验收评分：97.1%
   - 34/35 项通过
   - 详细验收结果
   - 问题修复记录
   - 技术债务分析

2. [PHASE1_2_ACCEPTANCE_CHECKLIST.md](PHASE1_2_ACCEPTANCE_CHECKLIST.md)
   - 35 项检查清单
   - 分类验收（基础设施、数据库、Redis、后端、前端等）
   - 验收标准说明

#### 🔧 开发参考
1. [dev-logs/development-log.md](dev-logs/development-log.md)
   - 编译问题修复（6 个）
   - 依赖包调整
   - 配置优化记录

---

## 📂 文档结构

```
docs/
├── STARTUP_GUIDE.md                    # 启动指南（575 行）
├── PHASE1_2_ACCEPTANCE_REPORT.md       # 验收报告（600+ 行）
├── PHASE1_2_ACCEPTANCE_CHECKLIST.md    # 验收清单（337 行）
└── dev-logs/                           # 开发日志
    ├── README.md                       # 日志说明
    └── development-log.md              # 开发日志（131 行）
```

---

## 🎯 文档使用指南

### 对于新开发者

**推荐阅读顺序**：
1. [STARTUP_GUIDE.md](STARTUP_GUIDE.md) - 快速启动和完整指南
2. [../specs/001-rag-recommendation-system/spec.md](../specs/001-rag-recommendation-system/spec.md) - 需求理解

### 对于项目验收

**验收检查清单**：
1. [PHASE1_2_ACCEPTANCE_CHECKLIST.md](PHASE1_2_ACCEPTANCE_CHECKLIST.md) - 逐项检查
2. [PHASE1_2_ACCEPTANCE_REPORT.md](PHASE1_2_ACCEPTANCE_REPORT.md) - 验收结论

### 对于故障排除

**问题解决步骤**：
1. 查看相应文档的"故障排除"章节
2. 查看 [dev-logs/development-log.md](dev-logs/development-log.md) - 类似问题
3. 搜索 GitHub Issues
4. 提交新的 Issue

---

## 📝 文档更新记录

### 2025-12-29

**新增文档**：
- ✅ `PHASE1_2_ACCEPTANCE_REPORT.md` - Phase 1-2 验收报告
- ✅ `README.md` - 本文档

**更新文档**：
- ✅ `STARTUP_GUIDE.md` - 整合并完善启动指南
- ✅ `dev-logs/development-log.md` - 添加最新修复记录

---

## 🔍 文档搜索

### 搜索关键词

**按功能搜索**：
- "启动" → STARTUP_GUIDE.md
- "验收" → PHASE1_2_ACCEPTANCE_*.md
- "配置" → STARTUP_GUIDE.md
- "故障" → 各文档的"故障排除"章节

**按组件搜索**：
- "Redis" → 验收报告 + 启动指南
- "数据库" → 验收报告 + 启动指南
- "认证" → 验收报告 + API 文档

---

## 🛠️ 文档维护

### 文档规范

- **格式**: Markdown（.md）
- **编码**: UTF-8
- **换行**: LF（Unix 风格）
- **最大行宽**: 100 字符（建议）

### 更新流程

1. 修改文档内容
2. 更新"文档更新记录"章节
3. 提交 PR 并说明变更

### 文档审查

- 技术准确性：由开发团队审查
- 格式规范：由文档维护者审查
- 定期更新：每个 Phase 结束后更新

---

## 📮 反馈与建议

如果您发现文档中的错误或有改进建议，请：

1. 提交 GitHub Issue
2. 直接提交 PR（文档更新）
3. 联系：1307792296@qq.com

---

**文档版本**: v1.0.0
**最后更新**: 2025-12-29
**维护者**: Aetheris RAG Team
