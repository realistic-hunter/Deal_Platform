from __future__ import annotations

import html
import zipfile
from datetime import datetime
from pathlib import Path


OUT = Path("校园二手交易平台需求分析.docx")


def esc(text: str) -> str:
    return html.escape(text, quote=False)


def r(text: str, bold: bool = False, color: str | None = None) -> str:
    props = []
    if bold:
        props.append("<w:b/><w:bCs/>")
    if color:
        props.append(f'<w:color w:val="{color}"/>')
    props.append('<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/>')
    rpr = f"<w:rPr>{''.join(props)}</w:rPr>"
    return f"<w:r>{rpr}<w:t xml:space=\"preserve\">{esc(text)}</w:t></w:r>"


def p(text: str = "", style: str = "Normal", align: str | None = None) -> str:
    align_xml = f'<w:jc w:val="{align}"/>' if align else ""
    return f'<w:p><w:pPr><w:pStyle w:val="{style}"/>{align_xml}</w:pPr>{r(text)}</w:p>'


def p_runs(parts: list[tuple[str, bool, str | None]], style: str = "Normal", align: str | None = None) -> str:
    align_xml = f'<w:jc w:val="{align}"/>' if align else ""
    runs = "".join(r(text, bold=bold, color=color) for text, bold, color in parts)
    return f'<w:p><w:pPr><w:pStyle w:val="{style}"/>{align_xml}</w:pPr>{runs}</w:p>'


def bullet(text: str) -> str:
    return (
        '<w:p><w:pPr><w:pStyle w:val="ListParagraph"/>'
        '<w:numPr><w:ilvl w:val="0"/><w:numId w:val="1"/></w:numPr></w:pPr>'
        f"{r(text)}</w:p>"
    )


def numbered(text: str) -> str:
    return (
        '<w:p><w:pPr><w:pStyle w:val="ListParagraph"/>'
        '<w:numPr><w:ilvl w:val="0"/><w:numId w:val="2"/></w:numPr></w:pPr>'
        f"{r(text)}</w:p>"
    )


def page_break() -> str:
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def cell(text: str, width: int, header: bool = False) -> str:
    fill = '<w:shd w:fill="F2F4F7"/>' if header else ""
    bold = header
    return (
        f'<w:tc><w:tcPr><w:tcW w:w="{width}" w:type="dxa"/>'
        '<w:tcMar><w:top w:w="80" w:type="dxa"/><w:bottom w:w="80" w:type="dxa"/>'
        '<w:start w:w="120" w:type="dxa"/><w:end w:w="120" w:type="dxa"/></w:tcMar>'
        f"{fill}</w:tcPr>"
        f'<w:p><w:pPr><w:spacing w:after="80" w:line="264" w:lineRule="auto"/></w:pPr>{r(text, bold=bold)}</w:p>'
        "</w:tc>"
    )


def table(rows: list[list[str]], widths: list[int]) -> str:
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths)
    body = []
    for i, row in enumerate(rows):
        cells = "".join(cell(text, widths[j], header=(i == 0)) for j, text in enumerate(row))
        body.append(f"<w:tr>{cells}</w:tr>")
    return (
        '<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/>'
        '<w:tblInd w:w="120" w:type="dxa"/>'
        '<w:tblBorders><w:top w:val="single" w:sz="4" w:color="D9E2EC"/>'
        '<w:left w:val="single" w:sz="4" w:color="D9E2EC"/>'
        '<w:bottom w:val="single" w:sz="4" w:color="D9E2EC"/>'
        '<w:right w:val="single" w:sz="4" w:color="D9E2EC"/>'
        '<w:insideH w:val="single" w:sz="4" w:color="D9E2EC"/>'
        '<w:insideV w:val="single" w:sz="4" w:color="D9E2EC"/></w:tblBorders>'
        '<w:tblLayout w:type="fixed"/></w:tblPr>'
        f"<w:tblGrid>{grid}</w:tblGrid>{''.join(body)}</w:tbl>"
    )


def styles_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/><w:qFormat/>
    <w:pPr><w:spacing w:after="120" w:line="264" w:lineRule="auto"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:sz w:val="22"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="TitleCustom">
    <w:name w:val="Title Custom"/><w:basedOn w:val="Normal"/><w:qFormat/>
    <w:pPr><w:spacing w:before="0" w:after="160"/><w:jc w:val="center"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:b/><w:bCs/><w:color w:val="0B2545"/><w:sz w:val="44"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="SubtitleCustom">
    <w:name w:val="Subtitle Custom"/><w:basedOn w:val="Normal"/>
    <w:pPr><w:spacing w:after="180"/><w:jc w:val="center"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:color w:val="666666"/><w:sz w:val="24"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="320" w:after="160"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:b/><w:bCs/><w:color w:val="2E74B5"/><w:sz w:val="32"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:b/><w:bCs/><w:color w:val="2E74B5"/><w:sz w:val="26"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="160" w:after="80"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:b/><w:bCs/><w:color w:val="1F4D78"/><w:sz w:val="24"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="ListParagraph">
    <w:name w:val="List Paragraph"/><w:basedOn w:val="Normal"/>
    <w:pPr><w:spacing w:after="160" w:line="280" w:lineRule="auto"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/><w:sz w:val="22"/></w:rPr>
  </w:style>
</w:styles>"""


def numbering_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:abstractNum w:abstractNumId="0">
    <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="bullet"/><w:lvlText w:val="•"/>
      <w:pPr><w:tabs><w:tab w:val="num" w:pos="720"/></w:tabs><w:ind w:left="720" w:hanging="360"/></w:pPr>
    </w:lvl>
  </w:abstractNum>
  <w:abstractNum w:abstractNumId="1">
    <w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/>
      <w:pPr><w:tabs><w:tab w:val="num" w:pos="720"/></w:tabs><w:ind w:left="720" w:hanging="360"/></w:pPr>
    </w:lvl>
  </w:abstractNum>
  <w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
  <w:num w:numId="2"><w:abstractNumId w:val="1"/></w:num>
</w:numbering>"""


def document_xml() -> str:
    today = datetime.now().strftime("%Y年%m月%d日")
    body: list[str] = []
    body.append(p("校园二手交易平台需求分析报告", "TitleCustom"))
    body.append(p("基于 Deal_Platform 项目源码与数据库脚本整理", "SubtitleCustom"))
    body.append(table([
        ["文档项", "内容"],
        ["项目名称", "Deal_Platform 校园二手交易平台"],
        ["文档类型", "软件需求分析说明"],
        ["生成日期", today],
        ["适用范围", "课程设计、项目答辩、开发交付与后续迭代说明"],
    ], [2200, 7160]))

    body.append(p("1. 项目概述", "Heading1"))
    body.append(p("本项目是一个面向校园场景的二手交易 Web 系统，主要解决学生之间闲置教材、电子产品、生活用品等物品的信息发布、浏览咨询、线下交易和后台监管问题。系统采用浏览器访问方式，普通用户完成商品交易相关操作，管理员负责平台内容审核、基础数据维护和运营管理。"))
    body.append(p("建设目标包括：降低校园闲置物品流转成本，规范商品发布和交易流程，提供可追踪的订单、评价、举报和通知机制，并通过后台统计与日志能力辅助管理员维护平台秩序。"))

    body.append(p("2. 用户角色分析", "Heading1"))
    body.append(table([
        ["角色", "主要目标", "典型操作"],
        ["游客", "浏览公开商品信息，决定是否注册登录。", "查看商品列表、查看商品详情、访问登录和注册页面。"],
        ["普通用户", "发布和购买二手商品，管理个人交易行为。", "注册登录、发布商品、编辑商品、收藏、留言、下单、取消订单、确认交易结果、评价。"],
        ["管理员", "维护平台内容和交易秩序，处理异常业务。", "商品审核、分类管理、公告管理、举报处理、日志查看、数据统计、轮播图维护、商品导出。"],
    ], [1600, 3600, 4160]))

    body.append(p("3. 功能性需求", "Heading1"))
    body.append(p("3.1 前台用户功能", "Heading2"))
    body.append(table([
        ["编号", "需求名称", "需求说明", "优先级"],
        ["FR-01", "用户注册与登录", "用户可通过用户名、密码、手机号、邮箱完成注册，并通过用户名和密码登录系统。登录后使用 Session 保持会话。", "高"],
        ["FR-02", "个人信息管理", "用户可查看和修改昵称、真实姓名、联系方式、邮箱等个人资料，并支持修改登录密码。", "高"],
        ["FR-03", "商品浏览与检索", "用户可分页浏览已上架商品，并按关键词、分类、排序条件筛选商品。", "高"],
        ["FR-04", "商品发布与维护", "登录用户可新增、编辑、删除自己的商品，填写标题、描述、价格、成色、交易地点、分类和图片。", "高"],
        ["FR-05", "商品收藏与浏览记录", "用户可收藏感兴趣商品，系统自动记录登录用户的商品浏览历史。", "中"],
        ["FR-06", "留言咨询", "用户可在商品详情页留言咨询，卖家或相关用户可基于留言进行回复。", "中"],
        ["FR-07", "订单交易", "买家可对商品创建订单，查看购买订单；卖家可查看出售订单并确认交易完成。", "高"],
        ["FR-08", "评价与举报", "交易完成后买家可提交评价；用户可对异常商品进行举报，管理员在后台处理。", "中"],
        ["FR-09", "站内通知", "系统可向用户发送审核、订单、留言等业务通知，用户可查看并标记已读。", "中"],
    ], [900, 1700, 5360, 1400]))

    body.append(p("3.2 后台管理功能", "Heading2"))
    body.append(table([
        ["编号", "需求名称", "需求说明", "优先级"],
        ["BR-01", "管理员权限控制", "只有角色为 admin 的登录用户可访问 /admin 下的后台管理功能。", "高"],
        ["BR-02", "商品审核管理", "管理员可查看商品列表，对商品进行审核通过、驳回、上下架和删除操作。", "高"],
        ["BR-03", "分类管理", "管理员可新增、修改商品分类，并控制分类启用状态和排序。", "中"],
        ["BR-04", "公告管理", "管理员可发布和维护系统公告，前台展示启用状态的公告。", "中"],
        ["BR-05", "举报处理", "管理员可查看用户举报并记录处理结果。", "中"],
        ["BR-06", "评价与日志查看", "管理员可查看交易评价和关键操作日志，用于追踪平台行为。", "中"],
        ["BR-07", "数据统计", "后台首页展示商品、订单、用户等统计卡片，以及分类和订单状态统计。", "中"],
        ["BR-08", "轮播图管理", "管理员可维护首页轮播图标题、图片地址、跳转链接、排序和状态。", "低"],
        ["BR-09", "数据导出", "管理员可导出商品数据 CSV，便于线下分析或归档。", "低"],
    ], [900, 1700, 5360, 1400]))

    body.append(p("4. 非功能性需求", "Heading1"))
    for item in [
        "易用性：页面应保持清晰的导航结构，普通用户能够快速完成浏览、发布、下单和查看订单等核心流程。",
        "安全性：核心页面必须校验登录状态；后台页面必须校验管理员角色；后端对关键参数进行二次校验。",
        "可靠性：商品、订单、评价、举报、通知等核心数据应持久化到 MySQL，并通过外键和约束保证基本一致性。",
        "可维护性：系统按 controller、service、model、interceptor、config 分层组织，便于后续扩展业务模块。",
        "性能要求：商品列表采用分页查询，避免一次性加载大量数据；常用查询字段设置索引。",
        "兼容性：系统基于浏览器访问，后端使用 Spring Boot 内嵌服务运行，适合本地开发和校园局域网部署。",
    ]:
        body.append(bullet(item))

    body.append(p("5. 数据需求", "Heading1"))
    body.append(p("系统数据库名称为 Deal_Platform，主要数据表覆盖用户、角色、商品、分类、商品图片、收藏、浏览记录、订单、订单状态日志、留言、评价、举报、公告、通知、操作日志、轮播图、支付记录、文件资源和数据字典等。"))
    body.append(table([
        ["数据对象", "关键字段/内容", "说明"],
        ["用户与角色", "user、role", "保存账号、角色、联系方式、状态、登录时间等信息。"],
        ["商品与分类", "goods、category、goods_image", "保存商品基础信息、价格、状态、图片、发布者和分类。"],
        ["互动数据", "favorite、browse_history、message", "保存收藏、浏览记录和商品留言回复。"],
        ["交易数据", "orders、order_status_log、review", "保存订单、状态流转、交易评价。"],
        ["治理数据", "report、notice、notification、operation_log", "保存举报、公告、站内消息和后台操作日志。"],
        ["运营数据", "banner、dict_type、dict_data、file_resource", "保存轮播图、字典项和上传资源信息。"],
    ], [1900, 2500, 4960]))

    body.append(p("6. 业务规则", "Heading1"))
    for item in [
        "未登录用户访问发布商品、个人中心、订单、收藏、历史记录、通知和后台路径时，应跳转到登录页。",
        "非管理员访问后台路径时，应跳转到无权限页面。",
        "普通用户只能修改和删除自己发布的商品；管理员可在后台管理商品状态。",
        "商品创建后进入待审核状态，审核通过后才能作为公开商品展示。",
        "同一用户对同一商品的收藏应保持唯一，重复操作可用于取消收藏。",
        "商品生成有效订单后，应避免重复创建冲突订单；订单取消后商品状态应恢复可交易。",
        "订单完成后才允许评价，且同一订单只允许提交一次评价。",
        "关键后台操作应写入操作日志，便于追踪和审计。",
    ]:
        body.append(numbered(item))

    body.append(p("7. 页面与交互需求", "Heading1"))
    body.append(table([
        ["页面/模块", "主要内容"],
        ["登录与注册", "登录表单、注册表单、错误提示、成功提示。"],
        ["商品列表", "轮播图、公告、分类筛选、关键词搜索、排序、分页、商品卡片。"],
        ["商品详情", "商品信息、图片、浏览/收藏状态、留言区、举报入口、下单入口、评价展示。"],
        ["我的商品", "当前用户发布商品列表、编辑、删除、状态查看。"],
        ["订单列表", "买家订单、卖家订单、订单状态、取消、完成、评价入口。"],
        ["后台首页", "统计卡片、分类统计、订单状态统计、最新商品。"],
        ["后台管理页", "商品、分类、公告、举报、评价、日志、轮播图等管理页面。"],
    ], [2200, 7160]))

    body.append(p("8. 开发与运行环境", "Heading1"))
    body.append(table([
        ["类别", "环境/技术"],
        ["开发语言", "Java 17"],
        ["后端框架", "Spring Boot 3.3.5、Spring MVC、Spring JDBC"],
        ["模板引擎", "Thymeleaf"],
        ["数据库", "MySQL，数据库名 Deal_Platform，字符集 utf8mb4"],
        ["构建工具", "Maven"],
        ["运行入口", "com.example.dealplatform.DealPlatformApplication"],
        ["默认端口", "8080"],
        ["配置文件", "src/main/resources/application.properties"],
        ["数据库脚本", "src/main/resources/sql/Deal_Platform.sql"],
    ], [2200, 7160]))

    body.append(p("9. 验收标准", "Heading1"))
    for item in [
        "能够完成数据库初始化，并成功启动 Spring Boot 应用。",
        "普通用户可完成注册、登录、发布商品、浏览商品、收藏、留言、下单和查看订单。",
        "管理员可进入后台并完成商品审核、分类维护、公告维护、举报处理、日志查看和数据统计。",
        "未登录和越权访问能够被拦截，页面跳转符合预期。",
        "商品列表分页、筛选和详情展示正常，图片上传和静态资源访问正常。",
        "订单状态流转、评价、通知和操作日志数据能够正确写入数据库。",
    ]:
        body.append(bullet(item))

    body.append(p("10. 后续扩展建议", "Heading1"))
    for item in [
        "引入 Spring Security 和密码加密，提升认证与授权安全性。",
        "将上传文件迁移到独立目录或对象存储，避免运行环境变更导致文件丢失。",
        "补充单元测试和集成测试，覆盖订单状态流转、商品审核、权限拦截等关键路径。",
        "增加站内搜索优化、消息实时提醒、交易信用分和违规用户封禁机制。",
    ]:
        body.append(bullet(item))

    body.append(
        '<w:sectPr><w:pgSz w:w="12240" w:h="15840"/>'
        '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>'
        '</w:sectPr>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" '
        'xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" '
        'xmlns:o="urn:schemas-microsoft-com:office:office" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
        'xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" '
        'xmlns:v="urn:schemas-microsoft-com:vml" '
        'xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" '
        'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" '
        'xmlns:w10="urn:schemas-microsoft-com:office:word" '
        'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
        'xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" '
        'xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" '
        'xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" '
        'xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" '
        'xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" '
        'mc:Ignorable="w14 wp14"><w:body>'
        + "".join(body)
        + "</w:body></w:document>"
    )


def write_docx() -> None:
    files = {
        "[Content_Types].xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>""",
        "_rels/.rels": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>""",
        "word/_rels/document.xml.rels": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
</Relationships>""",
        "word/document.xml": document_xml(),
        "word/styles.xml": styles_xml(),
        "word/numbering.xml": numbering_xml(),
        "word/settings.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:defaultTabStop w:val="720"/>
  <w:compat/>
</w:settings>""",
        "docProps/core.xml": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>校园二手交易平台需求分析报告</dc:title>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{datetime.utcnow().isoformat()}Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{datetime.utcnow().isoformat()}Z</dcterms:modified>
</cp:coreProperties>""",
        "docProps/app.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application>
</Properties>""",
    }
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
        for name, content in files.items():
            zf.writestr(name, content)


if __name__ == "__main__":
    write_docx()
    print(OUT.resolve())
