package com.accounting.app.util

/**
 * 分类常量定义。
 *
 * 集中管理支出一/二级分类与收入一/二级分类的合法取值，
 * 供 UI 下拉框、AI 校验、记忆比对等场景统一引用。
 */
object CategoryConstants {

    val expenseCategories = listOf("餐饮", "交通", "购物", "居家", "娱乐", "通讯", "医疗", "教育", "其他")

    val incomeCategories = listOf("工资", "奖金", "红包", "报销", "退款", "投资收益", "兼职收入", "其他收入")

    val expenseSubcategories = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "外卖", "奶茶", "零食", "咖啡饮品", "聚餐请客", "食材生鲜", "正餐", "饮品"),
        "交通" to listOf("公交", "地铁", "打车", "加油", "停车费", "共享单车", "火车高铁", "飞机", "长途汽车", "公共交通"),
        "购物" to listOf("日用品", "服饰鞋包", "数码电子", "家电", "美妆护肤", "宠物用品", "文具书籍", "网购综合", "食品生鲜"),
        "居家" to listOf("房租", "房贷", "水电燃气", "物业费", "网费电话费", "维修装修", "家居用品"),
        "娱乐" to listOf("游戏充值", "视频会员", "音乐音频", "电影演出", "运动健身", "旅游", "桌游剧本", "其他娱乐"),
        "通讯" to listOf("话费", "流量", "宽带", "手机费"),
        "医疗" to listOf("看病门诊", "药品", "住院", "体检", "牙科眼科", "保健品"),
        "教育" to listOf("课程培训", "书籍教材", "考试报名", "文具", "知识付费"),
        "其他" to listOf("快递费", "罚款", "公益捐款", "意外丢失")
    )

    val incomeSubcategories = mapOf(
        "工资" to listOf("基本工资", "加班补贴", "公积金提取"),
        "奖金" to listOf("年终奖", "绩效", "提成", "分红"),
        "红包" to listOf("节日红包", "礼金", "压岁钱"),
        "报销" to listOf("费用报销", "差旅报销"),
        "退款" to listOf("购物退款", "退货退款"),
        "投资收益" to listOf("股票盈利", "基金分红", "理财利息", "房租收入"),
        "兼职收入" to listOf("自由职业", "线上兼职", "线下兼职"),
        "其他收入" to listOf("二手卖出", "返利羊毛", "赔偿补偿", "意外所得")
    )

    /**
     * 内置场景词映射表：场景词 → (一级分类, 二级分类)。
     * 系统预置的通用分类规则，永远不写入用户记忆库。
     * 仅当用户记忆库 + 时间规则均未命中时才生效。
     */
    val builtinSceneMap = mapOf(
        // 餐饮
        "吃饭" to ("餐饮" to "正餐"), "饭" to ("餐饮" to "正餐"),
        "用餐" to ("餐饮" to "正餐"), "外卖" to ("餐饮" to "外卖"),
        "盒饭" to ("餐饮" to "正餐"), "小吃" to ("餐饮" to "零食"),
        "奶茶" to ("餐饮" to "饮品"), "咖啡" to ("餐饮" to "饮品"),
        "饮品" to ("餐饮" to "饮品"), "饮料" to ("餐饮" to "饮品"),
        "水" to ("餐饮" to "饮品"),

        // 交通
        "打车" to ("交通" to "打车"), "滴滴" to ("交通" to "打车"),
        "出租车" to ("交通" to "打车"), "网约车" to ("交通" to "打车"),
        "地铁" to ("交通" to "地铁"), "公交" to ("交通" to "公交"),
        "骑车" to ("交通" to "共享单车"),

        // 通讯
        "话费" to ("通讯" to "话费"), "流量" to ("通讯" to "流量"),
        "充话费" to ("通讯" to "话费"),

        // 居家
        "电费" to ("居家" to "水电燃气"), "水费" to ("居家" to "水电燃气"),
        "燃气费" to ("居家" to "水电燃气"), "房租" to ("居家" to "房租"),

        // 购物
        "水果" to ("购物" to "食品生鲜"), "零食" to ("购物" to "食品生鲜"),
        "超市" to ("购物" to "食品生鲜"),

        // 娱乐
        "会员" to ("娱乐" to "视频会员"), "电影" to ("娱乐" to "电影演出"),
    )

    fun getCategories(type: String): List<String> = if (type == "income") incomeCategories else expenseCategories

    fun getSubcategories(category: String): List<String> = expenseSubcategories[category] ?: incomeSubcategories[category] ?: emptyList()
}
