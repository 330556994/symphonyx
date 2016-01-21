/*
 * Copyright (c) 2012-2016, b3log.org & hacpai.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.Pagination;
import org.b3log.latke.model.Role;
import org.b3log.latke.model.User;
import org.b3log.latke.repository.CompositeFilter;
import org.b3log.latke.repository.CompositeFilterOperator;
import org.b3log.latke.repository.Filter;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.latke.util.Paginator;
import org.b3log.latke.util.Strings;
import org.b3log.symphony.model.Article;
import org.b3log.symphony.model.Comment;
import org.b3log.symphony.model.Common;
import org.b3log.symphony.model.Tag;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.processor.channel.ArticleChannel;
import org.b3log.symphony.repository.ArticleRepository;
import org.b3log.symphony.repository.CommentRepository;
import org.b3log.symphony.repository.TagArticleRepository;
import org.b3log.symphony.repository.TagRepository;
import org.b3log.symphony.repository.UserRepository;
import org.b3log.symphony.util.Emotions;
import org.b3log.symphony.util.Markdowns;
import org.b3log.symphony.util.Symphonys;
import org.b3log.symphony.util.Times;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

/**
 * Article query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.11.8.15, Dec 17, 2015
 * @since 0.2.0
 */
@Service
public class ArticleQueryService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleQueryService.class.getName());

    /**
     * Article repository.
     */
    @Inject
    private ArticleRepository articleRepository;

    /**
     * Comment repository.
     */
    @Inject
    private CommentRepository commentRepository;

    /**
     * Tag-Article repository.
     */
    @Inject
    private TagArticleRepository tagArticleRepository;

    /**
     * Tag repository.
     */
    @Inject
    private TagRepository tagRepository;

    /**
     * User repository.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Comment query service.
     */
    @Inject
    private CommentQueryService commentQueryService;

    /**
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * Avatar query service.
     */
    @Inject
    private AvatarQueryService avatarQueryService;

    /**
     * Short link query service.
     */
    @Inject
    private ShortLinkQueryService shortLinkQueryService;

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * Count to fetch article tags for relevant articles.
     */
    private static final int RELEVANT_ARTICLE_RANDOM_FETCH_TAG_CNT = 3;

    /**
     * Gets the relevant articles of the specified article with the specified fetch size.
     *
     * <p>
     * The relevant articles exist the same tag with the specified article.
     * </p>
     *
     * @param article the specified article
     * @param fetchSize the specified fetch size
     * @return relevant articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getRelevantArticles(final JSONObject article, final int fetchSize) throws ServiceException {
        final String tagsString = article.optString(Article.ARTICLE_TAGS);
        final String[] tagTitles = tagsString.split(",");
        final int tagTitlesLength = tagTitles.length;
        final int subCnt = tagTitlesLength > RELEVANT_ARTICLE_RANDOM_FETCH_TAG_CNT
                ? RELEVANT_ARTICLE_RANDOM_FETCH_TAG_CNT : tagTitlesLength;

        final List<Integer> tagIdx = CollectionUtils.getRandomIntegers(0, tagTitlesLength, subCnt);
        final int subFetchSize = fetchSize / subCnt;
        final Set<String> fetchedArticleIds = new HashSet<String>();

        final List<JSONObject> ret = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < tagIdx.size(); i++) {
                final String tagTitle = tagTitles[tagIdx.get(i)].trim();

                final JSONObject tag = tagRepository.getByTitle(tagTitle);
                final String tagId = tag.optString(Keys.OBJECT_ID);
                JSONObject result = tagArticleRepository.getByTagId(tagId, 1, subFetchSize);

                final JSONArray tagArticleRelations = result.optJSONArray(Keys.RESULTS);

                final Set<String> articleIds = new HashSet<String>();
                for (int j = 0; j < tagArticleRelations.length(); j++) {
                    final String articleId = tagArticleRelations.optJSONObject(j).optString(Article.ARTICLE + '_' + Keys.OBJECT_ID);

                    if (fetchedArticleIds.contains(articleId)) {
                        continue;
                    }

                    articleIds.add(articleId);
                    fetchedArticleIds.add(articleId);
                }

                final Query query = new Query().setFilter(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.IN, articleIds));
                result = articleRepository.get(query);

                ret.addAll(CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS)));
            }

            organizeArticles(ret);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets relevant articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets broadcasts (articles permalink equals to "aBroadcast").
     *
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getBroadcasts(final int currentPageNum, final int pageSize) throws ServiceException {
        try {
            final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize).setFilter(
                    new PropertyFilter(Article.ARTICLE_CLIENT_ARTICLE_ID, FilterOperator.EQUAL, "aBroadcast")).
                    addSort(Article.ARTICLE_CREATE_TIME, SortDirection.DESCENDING);

            final JSONObject result = articleRepository.get(query);
            final JSONArray articles = result.optJSONArray(Keys.RESULTS);

            if (0 == articles.length()) {
                return Collections.emptyList();
            }

            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(articles);
            for (final JSONObject article : ret) {
                article.put(Article.ARTICLE_PERMALINK, Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
                article.remove(Article.ARTICLE_CONTENT);
            }

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets broadcasts [currentPageNum=" + currentPageNum + ", pageSize=" + pageSize + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets interest articles.
     *
     * @param currentPageNum the specified current page number
     * @param pageSize the specified fetch size
     * @param tagTitles the specified tag titles
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getInterests(final int currentPageNum, final int pageSize, final String... tagTitles)
            throws ServiceException {
        try {
            final List<JSONObject> tagList = new ArrayList<JSONObject>();
            for (int i = 0; i < tagTitles.length; i++) {
                final String tagTitle = tagTitles[i];
                final JSONObject tag = tagRepository.getByTitle(tagTitle);
                if (null == tag) {
                    continue;
                }

                tagList.add(tag);
            }

            final Map<String, Class<?>> articleFields = new HashMap<String, Class<?>>();
            articleFields.put(Article.ARTICLE_TITLE, String.class);
            articleFields.put(Article.ARTICLE_PERMALINK, String.class);
            articleFields.put(Article.ARTICLE_CREATE_TIME, Long.class);

            final List<JSONObject> ret = new ArrayList<JSONObject>();

            if (!tagList.isEmpty()) {
                final List<JSONObject> tagArticles
                        = getArticlesByTags(currentPageNum, pageSize, articleFields, tagList.toArray(new JSONObject[0]));
                for (final JSONObject article : tagArticles) {
                    article.remove(Article.ARTICLE_T_PARTICIPANTS);
                    article.remove(Article.ARTICLE_T_PARTICIPANT_NAME);
                    article.remove(Article.ARTICLE_T_PARTICIPANT_THUMBNAIL_URL);
                    article.remove(Article.ARTICLE_LATEST_CMT_TIME);
                    article.remove(Article.ARTICLE_UPDATE_TIME);
                    article.remove(Article.ARTICLE_T_HEAT);
                    article.remove(Article.ARTICLE_T_TITLE_EMOJI);
                    article.remove(Common.TIME_AGO);

                    article.put(Article.ARTICLE_CREATE_TIME, ((Date) article.get(Article.ARTICLE_CREATE_TIME)).getTime());
                }

                ret.addAll(tagArticles);
            }

            final List<Filter> filters = new ArrayList<Filter>();
            filters.add(new PropertyFilter(Article.ARTICLE_STATUS, FilterOperator.EQUAL, Article.ARTICLE_STATUS_C_VALID));
            filters.add(new PropertyFilter(Article.ARTICLE_TYPE, FilterOperator.NOT_EQUAL, Article.ARTICLE_TYPE_C_DISCUSSION));

            final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING)
                    .setPageCount(currentPageNum).setPageSize(pageSize).setCurrentPageNum(1);
            query.setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters));
            for (final Map.Entry<String, Class<?>> articleField : articleFields.entrySet()) {
                query.addProjection(articleField.getKey(), articleField.getValue());
            }

            final JSONObject result = articleRepository.get(query);

            final List<JSONObject> recentArticles = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            ret.addAll(recentArticles);

            for (final JSONObject article : ret) {
                article.put(Article.ARTICLE_PERMALINK, Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
            }

            return ret;
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets interests failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets news (articles tags contains "B3log Announcement").
     *
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getNews(final int currentPageNum, final int pageSize) throws ServiceException {
        JSONObject tag = null;

        try {
            tag = tagRepository.getByTitle("B3log Announcement");
            if (null == tag) {
                return Collections.emptyList();
            }

            Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                    setFilter(new PropertyFilter(Tag.TAG + '_' + Keys.OBJECT_ID, FilterOperator.EQUAL, tag.optString(Keys.OBJECT_ID)))
                    .setPageCount(1).setPageSize(pageSize).setCurrentPageNum(currentPageNum);

            JSONObject result = tagArticleRepository.get(query);
            final JSONArray tagArticleRelations = result.optJSONArray(Keys.RESULTS);

            final Set<String> articleIds = new HashSet<String>();
            for (int i = 0; i < tagArticleRelations.length(); i++) {
                articleIds.add(tagArticleRelations.optJSONObject(i).optString(Article.ARTICLE + '_' + Keys.OBJECT_ID));
            }

            final JSONObject sa = userQueryService.getSA();

            final List<Filter> subFilters = new ArrayList<Filter>();
            subFilters.add(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.IN, articleIds));
            subFilters.add(new PropertyFilter(Article.ARTICLE_AUTHOR_EMAIL, FilterOperator.EQUAL, sa.optString(User.USER_EMAIL)));
            query = new Query().setFilter(new CompositeFilter(CompositeFilterOperator.AND, subFilters))
                    .addProjection(Article.ARTICLE_TITLE, String.class).addProjection(Article.ARTICLE_PERMALINK, String.class)
                    .addProjection(Article.ARTICLE_CREATE_TIME, Long.class).addSort(Article.ARTICLE_CREATE_TIME, SortDirection.DESCENDING);
            result = articleRepository.get(query);

            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            for (final JSONObject article : ret) {
                article.put(Article.ARTICLE_PERMALINK, Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
            }

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets news failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets articles by the specified tags (order by article create date desc).
     *
     * @param tags the specified tags
     * @param currentPageNum the specified page number
     * @param articleFields the specified article fields to return
     * @param pageSize the specified page size
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getArticlesByTags(final int currentPageNum, final int pageSize,
            final Map<String, Class<?>> articleFields, final JSONObject... tags) throws ServiceException {
        try {
            final List<Filter> filters = new ArrayList<Filter>();
            for (final JSONObject tag : tags) {
                filters.add(new PropertyFilter(Tag.TAG + '_' + Keys.OBJECT_ID, FilterOperator.EQUAL, tag.optString(Keys.OBJECT_ID)));
            }

            Filter filter;
            if (filters.size() >= 2) {
                filter = new CompositeFilter(CompositeFilterOperator.OR, filters);
            } else {
                filter = filters.get(0);
            }

            // XXX: 这里的分页是有问题的，后面取文章的时候会少（因为一篇文章可以有多个标签，但是文章 id 一样）
            Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                    setFilter(filter).setPageCount(1).setPageSize(pageSize).setCurrentPageNum(currentPageNum);

            JSONObject result = tagArticleRepository.get(query);
            final JSONArray tagArticleRelations = result.optJSONArray(Keys.RESULTS);

            final Set<String> articleIds = new HashSet<String>();
            for (int i = 0; i < tagArticleRelations.length(); i++) {
                articleIds.add(tagArticleRelations.optJSONObject(i).optString(Article.ARTICLE + '_' + Keys.OBJECT_ID));
            }

            query = new Query().setFilter(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.IN, articleIds)).
                    addSort(Keys.OBJECT_ID, SortDirection.DESCENDING);
            for (final Map.Entry<String, Class<?>> articleField : articleFields.entrySet()) {
                query.addProjection(articleField.getKey(), articleField.getValue());
            }

            result = articleRepository.get(query);

            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);

            final Integer participantsCnt = Symphonys.getInt("tagArticleParticipantsCnt");
            genParticipants(ret, participantsCnt);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets articles by tags [tagLength=" + tags.length + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets articles by the specified city (order by article create date desc).
     *
     * @param city the specified city
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getArticlesByCity(final String city, final int currentPageNum, final int pageSize)
            throws ServiceException {
        try {
            final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                    setFilter(new PropertyFilter(Article.ARTICLE_CITY, FilterOperator.EQUAL, city))
                    .setPageCount(1).setPageSize(pageSize).setCurrentPageNum(currentPageNum);

            final JSONObject result = articleRepository.get(query);

            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);

            final Integer participantsCnt = Symphonys.getInt("cityArticleParticipantsCnt");
            genParticipants(ret, participantsCnt);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets articles by city [" + city + "] failed", e);

            throw new ServiceException(e);
        }
    }

    /**
     * Gets articles by the specified tag (order by article create date desc).
     *
     * @param tag the specified tag
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getArticlesByTag(final JSONObject tag, final int currentPageNum, final int pageSize)
            throws ServiceException {
        try {
            Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                    setFilter(new PropertyFilter(Tag.TAG + '_' + Keys.OBJECT_ID, FilterOperator.EQUAL, tag.optString(Keys.OBJECT_ID)))
                    .setPageCount(1).setPageSize(pageSize).setCurrentPageNum(currentPageNum);

            JSONObject result = tagArticleRepository.get(query);
            final JSONArray tagArticleRelations = result.optJSONArray(Keys.RESULTS);

            final Set<String> articleIds = new HashSet<String>();
            for (int i = 0; i < tagArticleRelations.length(); i++) {
                articleIds.add(tagArticleRelations.optJSONObject(i).optString(Article.ARTICLE + '_' + Keys.OBJECT_ID));
            }

            query = new Query().setFilter(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.IN, articleIds)).
                    addSort(Keys.OBJECT_ID, SortDirection.DESCENDING);
            result = articleRepository.get(query);

            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);

            final Integer participantsCnt = Symphonys.getInt("tagArticleParticipantsCnt");
            genParticipants(ret, participantsCnt);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets articles by tag [tagTitle=" + tag.optString(Tag.TAG_TITLE) + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets an article by the specified client article id.
     *
     * @param authorId the specified author id
     * @param clientArticleId the specified client article id
     * @return article, return {@code null} if not found
     * @throws ServiceException service exception
     */
    public JSONObject getArticleByClientArticleId(final String authorId, final String clientArticleId) throws ServiceException {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Article.ARTICLE_CLIENT_ARTICLE_ID, FilterOperator.EQUAL, clientArticleId));
        filters.add(new PropertyFilter(Article.ARTICLE_AUTHOR_ID, FilterOperator.EQUAL, authorId));

        final Query query = new Query().setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters));
        try {
            final JSONObject result = articleRepository.get(query);
            final JSONArray array = result.optJSONArray(Keys.RESULTS);

            if (0 == array.length()) {
                return null;
            }

            return array.optJSONObject(0);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets article [clientArticleId=" + clientArticleId + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets an article with {@link #organizeArticle(org.json.JSONObject)} by the specified id.
     *
     * @param articleId the specified id
     * @return article, return {@code null} if not found
     * @throws ServiceException service exception
     */
    public JSONObject getArticleById(final String articleId) throws ServiceException {
        try {
            final JSONObject ret = articleRepository.get(articleId);

            if (null == ret) {
                return null;
            }

            organizeArticle(ret);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets an article [articleId=" + articleId + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets an article by the specified id.
     *
     * @param articleId the specified id
     * @return article, return {@code null} if not found
     * @throws ServiceException service exception
     */
    public JSONObject getArticle(final String articleId) throws ServiceException {
        try {
            final JSONObject ret = articleRepository.get(articleId);

            if (null == ret) {
                return null;
            }

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets an article [articleId=" + articleId + "] failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets the user articles with the specified user id, page number and page size.
     *
     * @param userId the specified user id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return user articles, return an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getUserArticles(final String userId, final int currentPageNum, final int pageSize) throws ServiceException {
        final Query query = new Query().addSort(Article.ARTICLE_CREATE_TIME, SortDirection.DESCENDING)
                .setCurrentPageNum(currentPageNum).setPageSize(pageSize).
                setFilter(new PropertyFilter(Article.ARTICLE_AUTHOR_ID, FilterOperator.EQUAL, userId));
        try {
            final JSONObject result = articleRepository.get(query);
            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets user articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets hot articles with the specified fetch size.
     *
     * @param fetchSize the specified fetch size
     * @return recent articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getHotArticles(final int fetchSize) throws ServiceException {
        final String id = String.valueOf(DateUtils.addDays(new Date(), -15).getTime());

        try {
            final Query query = new Query().addSort(Article.ARTICLE_COMMENT_CNT, SortDirection.DESCENDING).
                    addSort(Keys.OBJECT_ID, SortDirection.ASCENDING).setCurrentPageNum(1).setPageSize(fetchSize);

            final List<Filter> filters = new ArrayList<Filter>();
            filters.add(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.GREATER_THAN_OR_EQUAL, id));
            filters.add(new PropertyFilter(Article.ARTICLE_TYPE, FilterOperator.NOT_EQUAL, Article.ARTICLE_TYPE_C_DISCUSSION));

            query.setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters));

            final JSONObject result = articleRepository.get(query);
            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets hot articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets the random articles with the specified fetch size.
     *
     * @param fetchSize the specified fetch size
     * @return random articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getRandomArticles(final int fetchSize) throws ServiceException {
        try {
            final List<JSONObject> ret = articleRepository.getRandomly(fetchSize);
            organizeArticles(ret);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets random articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Makes article showing filters.
     *
     * @return filter the article showing to user
     */
    private CompositeFilter makeArticleShowingFilter() {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Article.ARTICLE_STATUS, FilterOperator.EQUAL, Article.ARTICLE_STATUS_C_VALID));
        filters.add(new PropertyFilter(Article.ARTICLE_TYPE, FilterOperator.NOT_EQUAL, Article.ARTICLE_TYPE_C_DISCUSSION));
        return new CompositeFilter(CompositeFilterOperator.AND, filters);
    }

    /**
     * Makes the recent (sort by create time) articles with the specified fetch size.
     *
     * @param currentPageNum the specified current page number
     * @param fetchSize the specified fetch size
     * @return recent articles query
     */
    private Query makeRecentQuery(final int currentPageNum, final int fetchSize) {
        final Query query = new Query()
                .addSort(Keys.OBJECT_ID, SortDirection.DESCENDING)
                .setPageCount(1).setPageSize(fetchSize).setCurrentPageNum(currentPageNum);
        query.setFilter(makeArticleShowingFilter());
        return query;
    }

    /**
     * Makes the top articles with the specified fetch size.
     *
     * @param currentPageNum the specified current page number
     * @param fetchSize the specified fetch size
     * @return top articles query
     */
    private Query makeTopQuery(final int currentPageNum, final int fetchSize) {
        final Query query = new Query()
                .addSort(Article.REDDIT_SCORE, SortDirection.DESCENDING)
                .addSort(Article.ARTICLE_LATEST_CMT_TIME, SortDirection.DESCENDING)
                .setPageCount(1).setPageSize(fetchSize).setCurrentPageNum(currentPageNum);

        query.setFilter(makeArticleShowingFilter());
        return query;
    }

    /**
     * Gets the recent (sort by create time) articles with the specified fetch size.
     *
     * @param currentPageNum the specified current page number
     * @param fetchSize the specified fetch size
     * @return recent articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getRecentArticles(final int currentPageNum, final int fetchSize) throws ServiceException {
        final Query query = makeRecentQuery(currentPageNum, fetchSize);

        try {
            final JSONObject result = articleRepository.get(query);
            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));

            organizeArticles(ret);

            for (final JSONObject article : ret) {
                final String authorId = article.optString(Article.ARTICLE_AUTHOR_ID);
                final JSONObject author = userRepository.get(authorId);
                if (UserExt.USER_STATUS_C_INVALID == author.optInt(UserExt.USER_STATUS)) {
                    article.put(Article.ARTICLE_TITLE, langPropsService.get("articleTitleBlockLabel"));
                }
            }

            final Integer participantsCnt = Symphonys.getInt("latestArticleParticipantsCnt");
            genParticipants(ret, participantsCnt);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets latest comment articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets the index articles with the specified fetch size.
     *
     * @param fetchSize the specified fetch size
     * @return recent articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getIndexArticles(final int fetchSize) throws ServiceException {
        final Query query = makeTopQuery(1, fetchSize);

        try {
            final JSONObject result = articleRepository.get(query);
            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));

            organizeArticles(ret);

            for (final JSONObject article : ret) {
                final String authorId = article.optString(Article.ARTICLE_AUTHOR_ID);
                final JSONObject author = userRepository.get(authorId);
                if (UserExt.USER_STATUS_C_INVALID == author.optInt(UserExt.USER_STATUS)) {
                    article.put(Article.ARTICLE_TITLE, langPropsService.get("articleTitleBlockLabel"));
                }
            }

            final Integer participantsCnt = Symphonys.getInt("indexArticleParticipantsCnt");
            genParticipants(ret, participantsCnt);

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets index articles failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Gets the recent articles with the specified fetch size.
     *
     * @param currentPageNum the specified current page number
     * @param fetchSize the specified fetch size
     * @return recent articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getRecentArticlesWithComments(final int currentPageNum, final int fetchSize) throws ServiceException {
        return getArticles(makeRecentQuery(currentPageNum, fetchSize));
    }

    /**
     * Gets the index articles with the specified fetch size.
     *
     * @param currentPageNum the specified current page number
     * @param fetchSize the specified fetch size
     * @return recent articles, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getTopArticlesWithComments(final int currentPageNum, final int fetchSize) throws ServiceException {
        return getArticles(makeTopQuery(currentPageNum, fetchSize));
    }

    /**
     * The specific articles.
     *
     * @param query conditions
     * @return articles
     * @throws ServiceException service exception
     */
    private List<JSONObject> getArticles(final Query query) throws ServiceException {
        try {
            final JSONObject result = articleRepository.get(query);
            final List<JSONObject> ret = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
            organizeArticles(ret);
            final List<JSONObject> stories = new ArrayList<JSONObject>();

            for (final JSONObject article : ret) {
                final JSONObject story = new JSONObject();
                final String authorId = article.optString(Article.ARTICLE_AUTHOR_ID);
                final JSONObject author = userRepository.get(authorId);
                if (UserExt.USER_STATUS_C_INVALID == author.optInt(UserExt.USER_STATUS)) {
                    story.put("title", langPropsService.get("articleTitleBlockLabel"));
                } else {
                    story.put("title", article.optString(Article.ARTICLE_TITLE));
                }
                story.put("id", article.optLong("oId"));
                story.put("url", Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
                story.put("user_display_name", article.optString(Article.ARTICLE_T_AUTHOR_NAME));
                story.put("user_job", author.optString(UserExt.USER_INTRO));
                story.put("comment_html", article.optString(Article.ARTICLE_CONTENT));
                story.put("comment_count", article.optInt(Article.ARTICLE_COMMENT_CNT));
                story.put("vote_count", article.optInt(Article.ARTICLE_GOOD_CNT));
                story.put("created_at", formatDate(article.get(Article.ARTICLE_CREATE_TIME)));
                story.put("user_portrait_url", article.optString(Article.ARTICLE_T_AUTHOR_THUMBNAIL_URL));
                story.put("comments", getAllComments(article.optString("oId")));
                final String tagsString = article.optString(Article.ARTICLE_TAGS);
                String[] tags = null;
                if (!Strings.isEmptyOrNull(tagsString)) {
                    tags = tagsString.split(",");
                }
                story.put("badge", tags == null ? "" : tags[0]);
                stories.add(story);
            }
            final Integer participantsCnt = Symphonys.getInt("indexArticleParticipantsCnt");
            genParticipants(stories, participantsCnt);
            return stories;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets index articles failed", e);
            throw new ServiceException(e);
        } catch (final JSONException ex) {
            LOGGER.log(Level.ERROR, "Gets index articles failed", ex);
            throw new ServiceException(ex);
        }
    }

    /**
     * Gets the article comments with the specified article id.
     *
     * @param articleId the specified article id
     * @return comments, return an empty list if not found
     * @throws ServiceException service exception
     * @throws JSONException json exception
     * @throws RepositoryException repository exception
     */
    private List<JSONObject> getAllComments(final String articleId) throws ServiceException, JSONException, RepositoryException {
        final List<JSONObject> commments = new ArrayList<JSONObject>();
        final List<JSONObject> articleComments = commentQueryService.getArticleComments(articleId, 1, Integer.MAX_VALUE);
        for (final JSONObject ac : articleComments) {
            final JSONObject comment = new JSONObject();
            final JSONObject author = userRepository.get(ac.optString(Comment.COMMENT_AUTHOR_ID));
            comment.put("id", ac.optLong("oId"));
            comment.put("body_html", ac.optString(Comment.COMMENT_CONTENT));
            comment.put("depth", 0);
            comment.put("user_display_name", ac.optString(Comment.COMMENT_T_AUTHOR_NAME));
            comment.put("user_job", author.optString(UserExt.USER_INTRO));
            comment.put("vote_count", 0);
            comment.put("created_at", formatDate(ac.get(Comment.COMMENT_CREATE_TIME)));
            comment.put("user_portrait_url", ac.optString(Comment.COMMENT_T_ARTICLE_AUTHOR_THUMBNAIL_URL));
            commments.add(comment);
        }
        return commments;
    }

    /**
     * The demand format date.
     *
     * @param date the original date
     * @return the format date like "2015-08-03T07:26:57Z"
     */
    private String formatDate(final Object date) {
        return DateFormatUtils.format(((Date) date).getTime(), "yyyy-MM-dd")
                + "T" + DateFormatUtils.format(((Date) date).getTime(), "HH:mm:ss") + "Z";
    }

    /**
     * Organizes the specified articles.
     *
     * <ul>
     * <li>converts create/update/latest comment time (long) to date type</li>
     * <li>generates author thumbnail URL</li>
     * <li>generates author name</li>
     * <li>escapes article title &lt; and &gt;</li>
     * <li>generates article heat</li>
     * <li>generates article view count display format(1k+/1.5k+...)</li>
     * <li>generates time ago text</li>
     * </ul>
     *
     * @param articles the specified articles
     * @throws RepositoryException repository exception
     */
    public void organizeArticles(final List<JSONObject> articles) throws RepositoryException {
        for (final JSONObject article : articles) {
            organizeArticle(article);
        }
    }

    /**
     * Organizes the specified article.
     *
     * <ul>
     * <li>converts create/update/latest comment time (long) to date type</li>
     * <li>generates author thumbnail URL</li>
     * <li>generates author name</li>
     * <li>escapes article title &lt; and &gt;</li>
     * <li>generates article heat</li>
     * <li>generates article view count display format(1k+/1.5k+...)</li>
     * <li>generates time ago text</li>
     * </ul>
     *
     * @param article the specified article
     * @throws RepositoryException repository exception
     */
    public void organizeArticle(final JSONObject article) throws RepositoryException {
        toArticleDate(article);
        genArticleAuthor(article);

        String title = article.optString(Article.ARTICLE_TITLE).replace("<", "&lt;").replace(">", "&gt;");
        title = Markdowns.clean(title, "");
        article.put(Article.ARTICLE_TITLE, title);

        article.put(Article.ARTICLE_T_TITLE_EMOJI, Emotions.convert(title));

        if (Article.ARTICLE_STATUS_C_INVALID == article.optInt(Article.ARTICLE_STATUS)) {
            article.put(Article.ARTICLE_TITLE, langPropsService.get("articleTitleBlockLabel"));
            article.put(Article.ARTICLE_T_TITLE_EMOJI, langPropsService.get("articleTitleBlockLabel"));
            article.put(Article.ARTICLE_CONTENT, langPropsService.get("articleContentBlockLabel"));
        }

        final String articleId = article.optString(Keys.OBJECT_ID);
        Integer viewingCnt = ArticleChannel.ARTICLE_VIEWS.get(articleId);
        if (null == viewingCnt) {
            viewingCnt = 0;
        }

        article.put(Article.ARTICLE_T_HEAT, viewingCnt);

        final int viewCnt = article.optInt(Article.ARTICLE_VIEW_CNT);
        final double views = (double) viewCnt / 1000;
        if (views >= 1) {
            final DecimalFormat df = new DecimalFormat("#.#");
            article.put(Article.ARTICLE_T_VIEW_CNT_DISPLAY_FORMAT, df.format(views) + "K");
        }
    }

    /**
     * Converts the specified article create/update/latest comment time (long) to date type.
     *
     * @param article the specified article
     */
    private void toArticleDate(final JSONObject article) {
        article.put(Common.TIME_AGO, Times.getTimeAgo(article.optLong(Article.ARTICLE_CREATE_TIME), Latkes.getLocale()));

        article.put(Article.ARTICLE_CREATE_TIME, new Date(article.optLong(Article.ARTICLE_CREATE_TIME)));
        article.put(Article.ARTICLE_UPDATE_TIME, new Date(article.optLong(Article.ARTICLE_UPDATE_TIME)));
        article.put(Article.ARTICLE_LATEST_CMT_TIME, new Date(article.optLong(Article.ARTICLE_LATEST_CMT_TIME)));
    }

    /**
     * Generates the specified article author name and thumbnail URL.
     *
     * @param article the specified article
     * @throws RepositoryException repository exception
     */
    private void genArticleAuthor(final JSONObject article) throws RepositoryException {
        final String authorEmail = article.optString(Article.ARTICLE_AUTHOR_EMAIL);

        if (Strings.isEmptyOrNull(authorEmail)) {
            return;
        }

        final JSONObject author = userRepository.getByEmail(authorEmail);

        article.put(Article.ARTICLE_T_AUTHOR_THUMBNAIL_URL, avatarQueryService.getAvatarURL(authorEmail));
        article.put(Article.ARTICLE_T_AUTHOR, author);

        article.put(Article.ARTICLE_T_AUTHOR_NAME, author.optString(User.USER_NAME));
    }

    /**
     * Generates participants for the specified articles.
     *
     * @param articles the specified articles
     * @param participantsCnt the specified generate size
     * @throws ServiceException service exception
     */
    private void genParticipants(final List<JSONObject> articles, final Integer participantsCnt) throws ServiceException {
        for (final JSONObject article : articles) {
            final String participantName = "";
            final String participantThumbnailURL = "";

            final List<JSONObject> articleParticipants
                    = getArticleLatestParticipants(article.optString(Keys.OBJECT_ID), participantsCnt);
            article.put(Article.ARTICLE_T_PARTICIPANTS, (Object) articleParticipants);

            article.put(Article.ARTICLE_T_PARTICIPANT_NAME, participantName);
            article.put(Article.ARTICLE_T_PARTICIPANT_THUMBNAIL_URL, participantThumbnailURL);
        }
    }

    /**
     * Gets the article participants (commenters) with the specified article article id and fetch size.
     *
     * @param articleId the specified article id
     * @param fetchSize the specified fetch size
     * @return article participants, for example,      <pre>
     * [
     *     {
     *         "articleParticipantName": "",
     *         "articleParticipantThumbnailURL": "",
     *         "articleParticipantThumbnailUpdateTime": long,
     *         "commentId": ""
     *     }, ....
     * ]
     * </pre>, returns an empty list if not found
     *
     * @throws ServiceException service exception
     */
    private List<JSONObject> getArticleLatestParticipants(final String articleId, final int fetchSize) throws ServiceException {
        final Query query = new Query().addSort(Comment.COMMENT_CREATE_TIME, SortDirection.DESCENDING)
                .setFilter(new PropertyFilter(Comment.COMMENT_ON_ARTICLE_ID, FilterOperator.EQUAL, articleId))
                .addProjection(Comment.COMMENT_AUTHOR_EMAIL, String.class).addProjection(Keys.OBJECT_ID, String.class)
                .setPageCount(1).setCurrentPageNum(1).setPageSize(fetchSize);
        final List<JSONObject> ret = new ArrayList<JSONObject>();

        try {
            final JSONObject result = commentRepository.get(query);
            final List<JSONObject> comments = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));

            for (final JSONObject comment : comments) {
                final String email = comment.optString(Comment.COMMENT_AUTHOR_EMAIL);
                final JSONObject commenter = userRepository.getByEmail(email);

                String thumbnailURL = Symphonys.get("defaultThumbnailURL");
                if (!UserExt.DEFAULT_CMTER_EMAIL.equals(email)) {
                    thumbnailURL = avatarQueryService.getAvatarURL(email);
                }

                final JSONObject participant = new JSONObject();
                participant.put(Article.ARTICLE_T_PARTICIPANT_NAME, commenter.optString(User.USER_NAME));
                participant.put(Article.ARTICLE_T_PARTICIPANT_THUMBNAIL_URL, thumbnailURL);
                participant.put(Article.ARTICLE_T_PARTICIPANT_THUMBNAIL_UPDATE_TIME,
                        commenter.optLong(UserExt.USER_UPDATE_TIME));
                participant.put(Article.ARTICLE_T_PARTICIPANT_URL, commenter.optString(User.USER_URL));
                participant.put(Comment.COMMENT_T_ID, comment.optString(Keys.OBJECT_ID));

                ret.add(participant);
            }

            return ret;
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets article [" + articleId + "] participants failed", e);
            throw new ServiceException(e);
        }
    }

    /**
     * Processes the specified article content.
     *
     * <ul>
     * <li>Generates &#64;username home URL</li>
     * <li>Markdowns</li>
     * <li>Generates secured article content</li>
     * <li>Blocks the article if need</li>
     * <li>Generates emotion images</li>
     * <li>Generates article link with article id</li>
     * </ul>
     *
     * @param article the specified article, for example,      <pre>
     * {
     *     "articleTitle": "",
     *     ....,
     *     "author": {}
     * }
     * </pre>
     *
     * @param request the specified request
     * @throws ServiceException service exception
     */
    public void processArticleContent(final JSONObject article, final HttpServletRequest request)
            throws ServiceException {
        final JSONObject author = article.optJSONObject(Article.ARTICLE_T_AUTHOR);
        if (null != author && UserExt.USER_STATUS_C_INVALID == author.optInt(UserExt.USER_STATUS)
                || Article.ARTICLE_STATUS_C_INVALID == article.optInt(Article.ARTICLE_STATUS)) {
            article.put(Article.ARTICLE_TITLE, langPropsService.get("articleTitleBlockLabel"));
            article.put(Article.ARTICLE_CONTENT, langPropsService.get("articleContentBlockLabel"));

            article.put(Article.ARTICLE_REWARD_CONTENT, "");
            article.put(Article.ARTICLE_REWARD_POINT, 0);

            return;
        }

        String articleContent = article.optString(Article.ARTICLE_CONTENT);
        article.put(Common.DISCUSSION_VIEWABLE, true);

        final Set<String> userNames = userQueryService.getUserNames(articleContent);
        final JSONObject currentUser = userQueryService.getCurrentUser(request);
        final String currentUserName = null == currentUser ? "" : currentUser.optString(User.USER_NAME);
        final String currentRole = null == currentUser ? "" : currentUser.optString(User.USER_ROLE);
        final String authorName = article.optString(Article.ARTICLE_T_AUTHOR_NAME);
        if (Article.ARTICLE_TYPE_C_DISCUSSION == article.optInt(Article.ARTICLE_TYPE)
                && !authorName.equals(currentUserName) && !Role.ADMIN_ROLE.equals(currentRole)) {
            boolean invited = false;
            for (final String userName : userNames) {
                if (userName.equals(currentUserName)) {
                    invited = true;

                    break;
                }
            }

            if (!invited) {
                String blockContent = langPropsService.get("articleDiscussionLabel");
                blockContent = blockContent.replace("{user}", "<a href='" + Latkes.getServePath()
                        + "/member/" + authorName + "'>" + authorName + "</a>");

                article.put(Article.ARTICLE_CONTENT, blockContent);
                article.put(Common.DISCUSSION_VIEWABLE, false);

                article.put(Article.ARTICLE_REWARD_CONTENT, "");
                article.put(Article.ARTICLE_REWARD_POINT, 0);

                return;
            }
        }

        for (final String userName : userNames) {
            articleContent = articleContent.replace('@' + userName, "@<a href='" + Latkes.getServePath()
                    + "/member/" + userName + "'>" + userName + "</a>");
        }

        articleContent = shortLinkQueryService.linkArticle(articleContent);
        articleContent = shortLinkQueryService.linkTag(articleContent);

        articleContent = Emotions.convert(articleContent);
        article.put(Article.ARTICLE_CONTENT, articleContent);

        if (article.optInt(Article.ARTICLE_REWARD_POINT) > 0) {
            String articleRewardContent = article.optString(Article.ARTICLE_REWARD_CONTENT);

            final Set<String> rewordContentUserNames = userQueryService.getUserNames(articleRewardContent);

            for (final String userName : rewordContentUserNames) {
                articleRewardContent = articleRewardContent.replace('@' + userName, "@<a href='" + Latkes.getServePath()
                        + "/member/" + userName + "'>" + userName + "</a>");
            }

            articleRewardContent = Emotions.convert(articleRewardContent);
            article.put(Article.ARTICLE_REWARD_CONTENT, articleRewardContent);
        }

        markdown(article);
    }

    /**
     * Gets articles by the specified request json object.
     *
     * @param requestJSONObject the specified request json object, for example,      <pre>
     * {
     *     "oId": "", // optional
     *     "paginationCurrentPageNum": 1,
     *     "paginationPageSize": 20,
     *     "paginationWindowSize": 10
     * }, see {@link Pagination} for more details
     * </pre>
     *
     * @param articleFields the specified article fields to return
     *
     * @return for example,      <pre>
     * {
     *     "pagination": {
     *         "paginationPageCount": 100,
     *         "paginationPageNums": [1, 2, 3, 4, 5]
     *     },
     *     "articles": [{
     *         "oId": "",
     *         "articleTitle": "",
     *         "articleContent": "",
     *         ....
     *      }, ....]
     * }
     * </pre>
     *
     * @throws ServiceException service exception
     * @see Pagination
     */
    public JSONObject getArticles(final JSONObject requestJSONObject, final Map<String, Class<?>> articleFields) throws ServiceException {
        final JSONObject ret = new JSONObject();

        final int currentPageNum = requestJSONObject.optInt(Pagination.PAGINATION_CURRENT_PAGE_NUM);
        final int pageSize = requestJSONObject.optInt(Pagination.PAGINATION_PAGE_SIZE);
        final int windowSize = requestJSONObject.optInt(Pagination.PAGINATION_WINDOW_SIZE);
        final Query query = new Query().setCurrentPageNum(currentPageNum).setPageSize(pageSize).
                addSort(Article.ARTICLE_UPDATE_TIME, SortDirection.DESCENDING);
        for (final Map.Entry<String, Class<?>> articleField : articleFields.entrySet()) {
            query.addProjection(articleField.getKey(), articleField.getValue());
        }

        if (requestJSONObject.has(Keys.OBJECT_ID)) {
            query.setFilter(new PropertyFilter(Keys.OBJECT_ID, FilterOperator.EQUAL, requestJSONObject.optString(Keys.OBJECT_ID)));
        }

        JSONObject result = null;

        try {
            result = articleRepository.get(query);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets articles failed", e);

            throw new ServiceException(e);
        }

        final int pageCount = result.optJSONObject(Pagination.PAGINATION).optInt(Pagination.PAGINATION_PAGE_COUNT);

        final JSONObject pagination = new JSONObject();
        ret.put(Pagination.PAGINATION, pagination);
        final List<Integer> pageNums = Paginator.paginate(currentPageNum, pageSize, pageCount, windowSize);
        pagination.put(Pagination.PAGINATION_PAGE_COUNT, pageCount);
        pagination.put(Pagination.PAGINATION_PAGE_NUMS, pageNums);

        final JSONArray data = result.optJSONArray(Keys.RESULTS);
        final List<JSONObject> articles = CollectionUtils.<JSONObject>jsonArrayToList(data);

        try {
            organizeArticles(articles);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Organizes articles failed", e);

            throw new ServiceException(e);
        }

        ret.put(Article.ARTICLES, articles);

        return ret;
    }

    /**
     * Markdowns the specified article content.
     *
     * <ul>
     * <li>Markdowns article content/reward content</li>
     * <li>Generates secured article content/reward content</li>
     * </ul>
     *
     * @param article the specified article content
     */
    private void markdown(final JSONObject article) {
        String content = article.optString(Article.ARTICLE_CONTENT);

        final int articleType = article.optInt(Article.ARTICLE_TYPE);
        if (Article.ARTICLE_TYPE_C_THOUGHT != articleType) {
            content = Markdowns.toHTML(content);
            content = Markdowns.clean(content, Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
        } else {
            final Document.OutputSettings outputSettings = new Document.OutputSettings();
            outputSettings.prettyPrint(false);

            content = Jsoup.clean(content, Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK),
                    Whitelist.relaxed().addAttributes(":all", "id", "target", "class").
                    addTags("span", "hr").addAttributes("iframe", "src", "width", "height")
                    .addAttributes("audio", "controls", "src"), outputSettings);

            content = content.replace("\n", "\\n").replace("'", "\\'")
                    .replace("\"", "\\\"");
        }

        article.put(Article.ARTICLE_CONTENT, content);

        if (article.optInt(Article.ARTICLE_REWARD_POINT) > 0) {
            String rewardContent = article.optString(Article.ARTICLE_REWARD_CONTENT);
            rewardContent = Markdowns.toHTML(rewardContent);
            rewardContent = Markdowns.clean(rewardContent,
                    Latkes.getServePath() + article.optString(Article.ARTICLE_PERMALINK));
            article.put(Article.ARTICLE_REWARD_CONTENT, rewardContent);
        }
    }
}
