# 数据库系统原理

## 第一章：数据库系统概述

数据库系统（DBS）是由数据库、数据库管理系统（DBMS）、应用系统和数据库管理员（DBA）组成的整体。

### 1.1 数据管理技术的发展

数据管理技术经历了三个阶段：

1. **人工管理阶段**：数据不保存，没有专门软件管理
2. **文件系统阶段**：数据可长期保存，但冗余度高
3. **数据库系统阶段**：数据结构化，共享性高，冗余度低

### 1.2 数据模型

#### 层次模型
用树形结构表示实体之间联系的模型。优点是查询效率高，缺点是修改复杂。

#### 网状模型
用图结构表示实体之间联系的模型。比层次模型更灵活，但结构复杂。

#### 关系模型
用二维表表示实体之间联系的模型。是最重要的数据模型。

```sql
-- 学生表
CREATE TABLE students (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT,
    department VARCHAR(50)
);

-- 课程表
CREATE TABLE courses (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    credit INT
);

-- 选课表
CREATE TABLE enrollments (
    student_id INT,
    course_id INT,
    grade DECIMAL(3,1),
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);
```

## 第二章：关系数据库

### 2.1 关系代数

关系代数是一种抽象的查询语言，包括：

- **选择** (σ)：从关系中选择满足条件的元组
- **投影** (π)：从关系中选择指定的属性
- **连接** (⋈)：将两个关系按一定条件连接
- **除** (÷)：表示"对于所有的"关系

### 2.2 SQL语言

SQL（Structured Query Language）是关系数据库的标准语言。

#### 数据定义语言 (DDL)

```sql
CREATE DATABASE university;

CREATE TABLE departments (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(100) UNIQUE,
    dean VARCHAR(50)
);

DROP TABLE departments;
```

#### 数据操作语言 (DML)

```sql
-- 插入数据
INSERT INTO students (id, name, age, department)
VALUES (1001, '张三', 20, '计算机');

-- 查询数据
SELECT s.name, c.name, e.grade
FROM students s
JOIN enrollments e ON s.id = e.student_id
JOIN courses c ON e.course_id = c.id
WHERE e.grade >= 85;

-- 更新数据
UPDATE students
SET age = 21
WHERE id = 1001;

-- 删除数据
DELETE FROM enrollments WHERE grade < 60;
```

## 第三章：数据库设计

### 3.1 范式理论

规范化是减少数据冗余、避免更新异常的过程。

#### 第一范式 (1NF)
每个属性都是不可分的原子值。

#### 第二范式 (2NF)
在1NF基础上，非主属性完全函数依赖于候选键。

#### 第三范式 (3NF)
在2NF基础上，非主属性不传递依赖于候选键。

#### BCNF (Boyce-Codd范式)
每个决定因素都是候选键。

### 3.2 ER模型设计

ER图（实体-联系图）是数据库设计的工具。

- **实体**：用矩形表示
- **属性**：用椭圆表示
- **联系**：用菱形表示

## 第四章：事务管理

### 4.1 事务的ACID特性

- **原子性** (Atomicity)：事务是不可分割的工作单位
- **一致性** (Consistency)：事务执行前后数据库状态一致
- **隔离性** (Isolation)：并发事务之间互不干扰
- **持久性** (Durability)：事务提交后，结果永久保存

### 4.2 并发控制

并发控制的主要技术包括：

- **锁机制**：共享锁(S锁)、排他锁(X锁)
- **时间戳排序**
- **多版本并发控制**(MVCC)

## 第五章：索引技术

### 5.1 索引类型

- **B+树索引**：最常用的索引结构
- **哈希索引**：适合等值查询
- **位图索引**：适合低基数列

### 5.2 查询优化

查询优化器的任务是为查询选择最高效的执行计划。

```sql
-- 创建索引
CREATE INDEX idx_student_name ON students(name);
CREATE INDEX idx_enrollment_grade ON enrollments(grade);

-- 查看执行计划
EXPLAIN SELECT * FROM students WHERE department = '计算机';
```

## 参考文献

1. Silberschatz, A., et al. *Database System Concepts*. 7th ed., McGraw-Hill, 2019.
2. Ramakrishnan, R., & Gehrke, J. *Database Management Systems*. 3rd ed., McGraw-Hill, 2002.
3. 王珊, 萨师煊. 《数据库系统概论》. 第5版, 高等教育出版社, 2014.
