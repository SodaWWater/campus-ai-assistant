# Java 程序设计与软件构造资料包

来源标注：

- MIT OpenCourseWare 6.005 Software Construction: https://ocw.mit.edu/courses/6-005-software-construction-spring-2016/
- Open Data Structures: https://opendatastructures.org/
- 北京大学 Java 程序设计课程页: https://www.icourse163.org/course/PKU-1001941004

## 课程目标

本资料包用于帮助学生理解 Java 编程、软件构造、测试、规格说明和抽象设计。资料内容为中文整理稿，参考公开课程主题，不复制大段原始材料。

## 关键主题

1. 正确性：程序不仅要能运行，还要能在边界条件下保持正确。
2. 可理解性：清晰的命名、模块边界和规格说明可以降低维护成本。
3. 可变性控制：避免共享可变状态可以减少隐藏 bug。
4. 测试设计：测试应覆盖正常情况、边界情况和异常情况。
5. 抽象：使用接口和规格说明隐藏实现细节。

## 示例问答素材

问题：为什么 Java 课程中要强调规格说明？

整理答案：规格说明描述方法对调用者的承诺，包括输入要求、输出结果、异常情况和副作用。它让调用者不用理解内部实现也能正确使用方法，同时让开发者能够基于明确契约编写测试。

问题：ArrayList 和 LinkedList 怎么选？

整理答案：ArrayList 基于动态数组，随机访问效率高，适合按下标读取和尾部追加。LinkedList 基于链表，理论上适合已定位节点附近的插入和删除，但实际开发中由于缓存局部性和额外对象开销，普通业务场景通常优先考虑 ArrayList。

