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
                    <a href="/post?type=0">
                        <div class="icon-article"></div>
                        ${articleLabel}
                    </a>
                    <div class="ft-fade">分享对别人有帮助的经验与见解</div>
                </div>
                <div>
                    <a href="/post?type=3">
                        <div class="icon-video"></div>
                        ${thoughtLabel}
                    </a>
                    <div class="ft-fade">
                        写作过程的记录与重放，文字版的沙画表演
                        <a href="http://hacpai.com/article/1441942422856" target="_blank">(?)</a>
                    </div>
                </div>
                <div>
                    <a href="/post?type=1">
                        <div class="icon-locked"></div>
                        ${discussionLabel}
                    </a>
                    <div class="ft-fade">@好友并在私密空间中进行交流</div>
                </div>
                <div>
                    <a href="/post?type=2">
                        <div class="icon-feed"></div>
                        ${cityBroadcastLabel}
                    </a>
                    <div class="ft-fade">发起你所在城市的招聘、Meetup 等，仅需<i>${broadcastPoint}</i> 积分</div>
                </div>
            </div>
        </div>
        <#include "../footer.ftl">
    </body>
</html>
