<#include "macro-home.ftl">
<#include "../macro-pagination.ftl">
<@home "points">
<table class="points">
    <#list userHomePoints as point>
    <tr<#if point_index % 2 == 1> class="even"</#if>>
        <td class="date">
            ${point.createTime?string('yyyy-MM-dd')} 
            <span class="ft-gray">${point.createTime?string('HH:mm')}</span>
        </td>
        <td class="name">${point.description}</td>
        <td class="responsive-hide sum<#if "+" == point.operation> plus">${point.sum?c}<#else>">-${point.sum?c}</#if></td>
        <td class="type responsive-hide">${point.displayType}</td>
        <td class="balance">${point.balance?c}</td>
    </tr>
    </#list>
</table>

<div class="fn-left" style="margin: 25px 0 10px">
    <font style="color: black;">♠</font> <a href="/top/balance">${pointLabel}${balanceLabel}</a>
    <font style="color: red;">♥</font> <a href="/charge/point">${chargePointLabel}</a>
    <font style="color: black;">♣</font> <a href="/top/checkin">${checkinTopLabel}</a>
    <font style="color: red;">♦</font> <a href="/top/consumption">${pointLabel}${consumptionLabel}</a>
</div>
<@pagination url="/member/${user.userName}/points"/>
</@home>