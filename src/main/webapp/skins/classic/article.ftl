<#include "macro-head.ftl">
<#include "macro-pagination.ftl">
<!DOCTYPE html>
<html>
    <head>
        <@head title="${article.articleTitle} - ${symphonyLabel}">
        <meta name="description" content="${article.articleTitle}"/>
        </@head>
        <link type="text/css" rel="stylesheet" href="${staticServePath}/js/lib/highlight.js-8.6/styles/github.css">
        <link type="text/css" rel="stylesheet" href="${staticServePath}/css/index${miniPostfix}.css?${staticResourceVersion}" />
        <link type="text/css" rel="stylesheet" href="${staticServePath}/js/lib/codemirror-5.3/codemirror.css" />
        <link type="text/css" rel="stylesheet" href="${staticServePath}/js/lib/codemirror-5.3/addon/hint/show-hint.css" />
    </head>
    <body>
        <#include "header.ftl">
        <div class="main">
            <div class="wrapper">
                <div class="content">
                    <div class="ft-gray fn-clear article-info">
                        <div class="fn-left">
                            <#list article.articleTags?split(",") as articleTag>
                            <a rel="tag" class="tag" href="/tags/${articleTag?url('UTF-8')}">
                                ${articleTag}
                            </a>&nbsp;
                            </#list>
                        </div>
                        <div class="responsive-show fn-hr5"></div>
                        <div class="fn-right">
                            <span class="icon-date"></span>
                            ${article.articleCreateTime?string('yyyy-MM-dd HH:mm')} &nbsp;
                            <a title="${cmtLabel}" rel="nofollow" href="#comments">
                                <span class="icon-cmts"></span>
                                ${article.articleCommentCount}
                            </a> &nbsp;
                            <span title="${viewLabel}"> 
                                <span class="icon-view"></span>
                                <#if article.articleViewCount < 1000>
                                ${article.articleViewCount}
                                <#else>
                                ${article.articleViewCntDisplayFormat}
                                </#if>
                            </span>
                            &nbsp;
                            <#if isLoggedIn>
                            <#if isFollowing>
                            <span class="ft-red fn-pointer" title="${uncollectLabel}" onclick="Util.unfollow(this, '${article.oId}', 'article')">
                                <span class="icon-star"></span>
                                ${article.articleCollectCnt}
                            </span>
                            <#else>
                            <span class="fn-pointer" title="${collectLabel}" onclick="Util.follow(this, '${article.oId}', 'article')">
                                <span class="icon-star"></span>
                                ${article.articleCollectCnt}
                            </span>
                            </#if>
                            <#else>
                            <span title="${collectLabel}" class="fn-pointer">
                                <span class="icon-star"></span>
                                ${article.articleCollectCnt}
                            </span>
                            </#if>
                        </div>
                    </div>
                    <div class="article-title fn-flex">
                        <h2 class="fn-flex-1">
                            <a rel="author" href="/member/${article.articleAuthorName}" class="ft-gray"
                               title="${article.articleAuthorName}">
                                <img class="avatar-small" src="${article.articleAuthorThumbnailURL}-64.jpg?${article.articleAuthor.userUpdateTime?c}" />
                            </a> &nbsp;
                            <#if 1 == article.articleType>
                            <span class="icon-locked" title="${discussionLabel}"></span>
                            <#elseif 2 == article.articleType>
                            <span class="icon-feed" title="${cityBroadcastLabel}"></span>
                            <#elseif 3 == article.articleType>
                            <span class="icon-video" title="${thoughtLabel}"></span>
                            </#if>
                            <a href="${article.articlePermalink}" rel="bookmark">
                                ${article.articleTitleEmoj}
                            </a> &nbsp;
                        </h2> 
                        <div class="responsive-show fn-hr5"></div>
                        <span>
                            <#if isLoggedIn>
                            <span id="voteUp" class="fn-pointer<#if 0==vote> ft-red</#if>" title="${upLabel} ${article.articleGoodCnt}" onclick="Util.voteUp('${article.oId}', 'article')">
                                <span class="icon-chevron-up"></span></span>&nbsp;
                            <span id="voteDown" class="fn-pointer<#if 1==vote> ft-red</#if>" title="${downLabel} ${article.articleBadCnt}" onclick="Util.voteDown('${article.oId}', 'article')">
                                <span class="icon-chevron-down"></span></span>
                            </#if>

                            <#if article.isMyArticle && 3 != article.articleType>
                            &nbsp;
                            <a href="/update?id=${article.oId}" title="${editLabel}" class="icon-edit"></a>
                            &nbsp;
                            </#if>
                            <#if isAdminLoggedIn>
                            <a class="icon-setting" href="/admin/article/${article.oId}" title="${adminLabel}"></a>
                            </#if>
                        </span>
                    </div>

                    <#if 3 != article.articleType>
                    <div class="content-reset article-content">${article.articleContent}</div>
                    <#else>
                    <div id="thoughtProgress"><div></div></div>
                    <div class="content-reset article-content"></div>
                    </#if>

                    <div class="fn-clear">
                        <div class="share fn-right">
                            <span class="icon-tencent" data-type="tencent"></span>
                            <span class="icon-weibo" data-type="weibo"></span>
                            <span class="icon-twitter" data-type="twitter"></span>
                            <span class="icon-google" data-type="google"></span>
                        </div>
                    </div>
                    <#if 0 < article.articleRewardPoint>
                    <div id="articleRewardContent"<#if !article.rewarded> class="reward"</#if>>
                         <#if !article.rewarded>
                         <span>
                            ${rewardTipLabel?replace("{articleId}", article.oId)?replace("{point}", article.articleRewardPoint)}
                        </span>
                        <#else>
                        ${article.articleRewardContent}
                        </#if>
                    </div>
                    </#if>
                    <#if isLoggedIn>
                    <#if discussionViewable && article.articleCommentable>
                    <div class="form fn-clear comment-wrap">
                        <form class="fn-none" id="fileUpload" method="POST" enctype="multipart/form-data">
                            <input type="file" name="file">
                        </form>
                        <textarea id="commentContent" placeholder="${commentEditorPlaceholderLabel}"></textarea>
                        <div class="tip" id="addCommentTip"></div>

                        <div class="fn-clear comment-submit">
                            <span class="responsive-hide">    
                                Markdown
                                <a href="javascript:void(0)" onclick="$('.grammar').slideToggle()">${baseGrammarLabel}</a>
                                <a target="_blank" href="http://daringfireball.net/projects/markdown/syntax">${allGrammarLabel}</a>
                                |
                                <a target="_blank" href="http://www.emoji-cheat-sheet.com">Emoji</a>
                            </span>
                            <div class="fn-right">
                                <button class="green" onclick="Comment.preview()">${previewLabel}</button> &nbsp; &nbsp; 
                                <button class="red" onclick="Comment.add('${article.oId}', '${csrfToken}')">${submitLabel}</button>
                            </div>
                        </div>
                        
                    </div>
                    <div class="grammar fn-none fn-clear">
                        ${markdwonGrammarLabel}
                    </div>
                    </#if>
                    <#else>
                    <div class="comment-login">
                        <a rel="nofollow" href="javascript:window.scrollTo(0,0);Util.showLogin();">${loginDiscussLabel}</a>
                    </div>
                    </#if>
                    <div class="fn-clear">
                        <div class="list" id="comments">
                            <h2>${article.articleCommentCount} ${cmtLabel}</h2>
                            <ul>
                                <#list article.articleComments as comment>
                                <li id="${comment.oId}">
                                    <div class="fn-flex">
                                        <a rel="nofollow" href="/member/${comment.commentAuthorName}">
                                            <img class="avatar" 
                                                 title="${comment.commentAuthorName}" src="${comment.commentAuthorThumbnailURL}-64.jpg?${comment.commenter.userUpdateTime?c}" />
                                        </a>
                                        <div class="fn-flex-1 comment-content">
                                            <div class="fn-clear comment-info">
                                                <span class="fn-left">
                                                    <a rel="nofollow" href="/member/${comment.commentAuthorName}"
                                                       title="${comment.commentAuthorName}">${comment.commentAuthorName}</a>
                                                    <span class="ft-fade ft-smaller">&nbsp;•&nbsp;${comment.timeAgo}</span>
                                                    <#if comment.rewardedCnt gt 0>
                                                    <span class="icon-heart ft-smaller ft-fade"></span> <span class="ft-smaller ft-fade" id='${comment.oId}RewardedCnt'>${comment.rewardedCnt}</span> 
                                                    </#if>
                                                </span>
                                                <span class="fn-right">
                                                    <#if isLoggedIn>
                                                    <#if comment.commentAuthorId != currentUser.oId>
                                                    <#if comment.rewarded>
                                                    <span class='ft-smaller ft-fade'>${thankedLabel}</span>
                                                    <#else>
                                                    <span class='fn-none thx fn-pointer ft-smaller ft-fade' id='${comment.oId}Thx'
                                                          onclick="Comment.thank('${comment.oId}', '${csrfToken}', '${comment.commentThankLabel}', '${thankedLabel}')">${thankLabel}</span>
                                                    </#if>
                                                    </#if>
                                                    <span class="icon-reply fn-pointer" onclick="Comment.replay('@${comment.commentAuthorName} ')"></span>
                                                    </#if>
                                                    <#if isAdminLoggedIn>
                                                    <a class="icon-setting" href="/admin/comment/${comment.oId}" title="${adminLabel}"></a>
                                                    </#if>
                                                    #<i>${article.articleCommentCount - ((paginationCurrentPageNum - 1) * articleCommentsPageSize + comment_index)}</i>
                                                </span>    
                                            </div>
                                            <div class="content-reset comment">
                                                ${comment.commentContent}
                                            </div>
                                        </div>
                                    </div>
                                </li>
                                </#list>  
                            </ul>
                        </div>
                        <@pagination url=article.articlePermalink/>
                    </div>
                </div>
                <div class="side">

                    <#include 'common/person-info.ftl'/>

                    <div class="module">
                        <div class="module-header">
                            <h2>
                                ${relativeArticleLabel}
                            </h2>
                        </div>
                        <div class="module-panel">
                            <ul class="module-list">
                                <#list sideRelevantArticles as relevantArticle>
                                <li<#if !relevantArticle_has_next> class="last"</#if>>
                                    <a rel="nofollow" href="${relevantArticle.articlePermalink}">${relevantArticle.articleTitle}</a>
                                    <a class="ft-gray" rel="nofollow" 
                                       href="/member/${relevantArticle.articleAuthorName}">${relevantArticle.articleAuthorName}</a>
                                </li>
                                </#list>
                            </ul>
                        </div>
                    </div>

                    <div class="module">
                        <div class="module-header">
                            <h2>
                                ${randomArticleLabel}
                            </h2>
                        </div>
                        <div class="module-panel">
                            <ul class="module-list">
                                <#list sideRandomArticles as randomArticle>
                                <li<#if !randomArticle_has_next> class="last"</#if>>
                                    <a rel="nofollow" href="${randomArticle.articlePermalink}">${randomArticle.articleTitle}</a>
                                    <a class="ft-gray" rel="nofollow"
                                       href="/member/${randomArticle.articleAuthorName}">${randomArticle.articleAuthorName}</a>
                                </li>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div id="heatBar">
            <i class="heat" style="width:${article.articleHeat*3}px"></i>
        </div>
        <#include "footer.ftl">
        <div id="preview" class="content-reset"></div>
        <script>
                    Label.commentErrorLabel = "${commentErrorLabel}";
                    Label.symphonyLabel = "${symphonyLabel}";
                    Label.rewardConfirmLabel = "${rewardConfirmLabel?replace("{point}", article.articleRewardPoint)}"
                    Label.articleOId = "${article.oId}";
                    Label.articleTitle = "${article.articleTitle}";
                    Label.articlePermalink = "${article.articlePermalink}";
                    Label.recordDeniedLabel = "${recordDeniedLabel}";
                    Label.recordDeviceNotFoundLabel = "${recordDeviceNotFoundLabel}";
                    Label.csrfToken = "${csrfToken}";</script>
        <script src="${staticServePath}/js/lib/jquery/jquery.bowknot.min.js"></script>
        <script src="${staticServePath}/js/lib/codemirror-5.3/codemirror.js"></script>
        <script src="${staticServePath}/js/lib/codemirror-5.3/mode/markdown/markdown.js"></script>
        <script src="${staticServePath}/js/lib/codemirror-5.3/addon/display/placeholder.js"></script>
        <script src="${staticServePath}/js/overwrite/codemirror/addon/hint/show-hint.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/highlight.js-8.6/highlight.pack.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/ws-flash/swfobject.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/ws-flash/web_socket.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/reconnecting-websocket.min.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/jquery/file-upload-9.10.1/vendor/jquery.ui.widget.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/jquery/file-upload-9.10.1/jquery.iframe-transport.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/jquery/file-upload-9.10.1/jquery.fileupload.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/jquery/file-upload-9.10.1/jquery.fileupload-process.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/jquery/file-upload-9.10.1/jquery.fileupload-validate.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/lib/sound-recorder/SoundRecorder.js"></script>
        <script type="text/javascript" src="${staticServePath}/js/article${miniPostfix}.js?${staticResourceVersion}"></script>
        <script type="text/javascript" src="${staticServePath}/js/channel${miniPostfix}.js?${staticResourceVersion}"></script>
        <script type="text/javascript" src="${staticServePath}/js/audio${miniPostfix}.js?${staticResourceVersion}"></script>
        <script>
                    WEB_SOCKET_SWF_LOCATION = "${staticServePath}/js/lib/ws-flash/WebSocketMain.swf";
                    // Init [Article] channel
                    ArticleChannel.init("ws://${serverHost}:${serverPort}/article-channel?articleId=${article.oId}&articleType=${article.articleType}");
                    // jQuery File Upload
                    Util.uploadFile({
                    "type": "img",
                            "id": "fileUpload",
                            "pasteZone": $(".CodeMirror"),
                            "qiniuUploadToken": "${qiniuUploadToken}",
                            "editor": Comment.editor,
                            "uploadingLabel": "${uploadingLabel}",
                            "qiniuDomain": "${qiniuDomain}"
                    });
                    var qiniuToken = '${qiniuUploadToken}';
                    var qiniuDomain = '${qiniuDomain}';
                    var audioRecordingLabel = '${audioRecordingLabel}';
                    var uploadingLabel = '${uploadingLabel}';
                    <#if 3 == article.articleType >
                    Article.playThought('${article.articleContent}');
                    </#if>
        </script>
    </body>
</html>
