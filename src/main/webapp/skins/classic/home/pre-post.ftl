<#include "../macro-head.ftl">
<!DOCTYPE html>
<html>
    <head>
        <@head title="${symphonyLabel} - ${selectAddTypeLabel}">
        <meta name="robots" content="none" />
        </@head>
        <link type="text/css" rel="stylesheet" href="${staticServePath}/css/home${miniPostfix}.css?${staticResourceVersion}" />
    </head>
    <body>
        <#include "../header.ftl">
        <div class="main">
            <div class="wrapper pre-post">
                <div>
                    <a href="/post?type=4&tags=航海日记,段落">
                        <div class="icon-navigation"></div>
                        ${journalLabel}
                    </a>
                    <div class="ft-fade"><span class="ft-green">记录</span>下今天的航海冒险经历</div>
                </div>
                <div>
                    <a href="/post?type=0">
                        <div class="icon-article"></div>
                        ${articleLabel}
                    </a>
                    <div class="ft-fade"><span class="ft-green">提问</span>或<span class="ft-green">分享</span>对别人有帮助的经验与见解</div>
                </div>
                <div>
                    <a href="/post?type=1">
                        <div class="icon-locked"></div>
                        ${discussionLabel}
                    </a>
                    <div class="ft-fade">@好友并在<span class="ft-red">私密</span>空间中进行交流</div>
                </div>
                <div>
                    <a href="/post?type=3">
                        <div class="icon-video"></div>
                        ${thoughtLabel}
                    </a>
                    <div class="ft-fade">
                        写作过程的记录与重放，文字版的<span class="ft-red">沙画</span>表演
                        <a href="http://hacpai.com/article/1441942422856" target="_blank">(?)</a>
                    </div>
                </div>
            </div>
        </div>
        <#include "../footer.ftl">
    </body>
</html>
