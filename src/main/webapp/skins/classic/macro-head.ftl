<#macro head title>
<meta charset="utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" />
<title>${title}</title>
<#nested>
<meta name="copyright" content="B3log" />
<meta http-equiv="Window-target" content="_top" />
<link type="text/css" rel="stylesheet" href="${staticServePath}/css/base${miniPostfix}.css?${staticResourceVersion}" />
<link type="text/css" rel="stylesheet" href="${staticServePath}/css/responsive${miniPostfix}.css?${staticResourceVersion}" />
<link rel="icon" type="image/x-icon" href="${staticServePath}/images/favicon.ico" />
${siteVisitStatCode}
</#macro>