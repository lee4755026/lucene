package com.xhemss.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchTest {

    private Directory directory;
    private IndexReader reader;
    private IndexSearcher indexSearcher;
    private IKAnalyzer analyzer;

    @Before
    public void setUp() throws Exception {
        directory =FSDirectory.open(Paths.get("E:\\xhemss\\lucene\\dataindex"));
        reader=DirectoryReader.open(directory);
        indexSearcher =new IndexSearcher(reader);
        analyzer=new IKAnalyzer(false);
    }

    @After
    public void tearDown() throws Exception {
        reader.close();
    }

    @Test
    public void testCreateIndex() throws IOException {
        Analyzer analyzer = new IKAnalyzer();
        //实例化索引配置，用来存放分析器
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        //实例化索引写入对象
        IndexWriter indexWriter;

        indexWriter = new IndexWriter(directory, indexWriterConfig);
        //清除以前的index
        indexWriter.deleteAll();

        List<String[]> dataList= new ArrayList<>();
        //{id,dispose_type,item_id,chapter,content,min_score,max_score}
        dataList.add(new String[]{"1","1","5","第八章第五十五条第一款","对违法违纪行为知情不报，情节轻微的，扣10分","10","10"});
        dataList.add(new String[]{"132","1","5","第八章第五十五条第一款","其他违反教育改造规范，情节严重的，扣25分","25","25"});
        dataList.add(new String[]{"133","1","5","第八章第五十五条第二款","对违法违纪行为知情不报的，不制止的，情节轻微的，扣30分","30","30"});
        dataList.add(new String[]{"134","1","5","第八章第五十五条第二款","对违法违纪行为知情不报的，不制止的，情节较重的，扣40分","40","40"});
        dataList.add(new String[]{"135","1","5","第八章第五十五条第二款","对违法违纪行为知情不报的，不制止的，情节严重的，扣50分","50","50"});

        List<Document> documentList=getDocuments(dataList);
        for(Document document:documentList){
            indexWriter.addDocument(document);
        }
        //关闭索引写入对象
        indexWriter.close();
    }

    private List<Document> getDocuments(List<String[]> dataList) {
        List<Document> list=new ArrayList<>();
        Document document;
        for(String[] values:dataList){
            //创建Document 文档对象
            document=new Document();
            //{id,dispose_type,item_id,chapter,content,min_score,max_score}
            document.add(new Field("id", values[0], TextField.TYPE_STORED));
            document.add(new Field("disposeType", values[1], TextField.TYPE_STORED));
            document.add(new Field("itemId", values[2], TextField.TYPE_STORED));
            document.add(new Field("chapter", values[3], TextField.TYPE_STORED));
            document.add(new Field("content", values[4], TextField.TYPE_STORED));
            document.add(new Field("minScore", values[5], TextField.TYPE_STORED));
            document.add(new Field("maxScore", values[6], TextField.TYPE_STORED));

            list.add(document);
        }
        return list;
    }

    /**
     * 对特定单词查询及模糊查询
     *
     * @throws Exception
     */
    @Test
    public void testTermQuery() throws Exception {
        String searchField = "content";
        // 所给出的必须是单词，不然差不到
        String q = "情节轻微";
        // 一个Term表示来自文本的一个单词。
        Term t = new Term(searchField, q);
        // 为Term构造查询。
        Query query = new TermQuery(t);

        TopDocs hits = indexSearcher.search(query, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        System.out.println("匹配 '" + q + "'，总共查询到" + hits.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("id"));
        }

        /**
         * 1.需要根据条件查询
         *
         * 2.最大可编辑数，取值范围0，1，2
         * 允许我的查询条件的值，可以错误几个字符
         *
         */
        Query query2 = new FuzzyQuery(new Term(searchField,"情节轻微"),2);
        TopDocs hits2 = indexSearcher.search(query2, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        System.out.println("匹配 '" + "知情不报"+ "'，总共查询到" + hits2.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("id"));
        }

        QueryParser qp = new QueryParser(searchField, new IKAnalyzer());
        qp.setDefaultOperator(QueryParser.OR_OPERATOR);
        Query parseQuery = qp.parse("对违法行为知情不报，不制止，情节严重。");
        //搜索相似度最高的5条记录
        TopDocs topDocs = indexSearcher.search(parseQuery, 5);
        System.out.println("命中：" + topDocs.totalHits);
        //输出结果
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (int i = 0; i < topDocs.totalHits; i++) {
            Document targetDoc = indexSearcher.doc(scoreDocs[i].doc);
//            System.out.println("内容：" + targetDoc.toString());
            System.out.println(targetDoc.get("id"));
        }

    }

    /**
     * 解析查询表达式
     *
     * @throws Exception
     */
    @Test
    public void testQueryParser() throws Exception {
        // 标准分词器
        Analyzer analyzer = new SmartChineseAnalyzer();
        String searchField = "address";
        String q = "中国广州";
        String q2 = "上海广州人民欢迎你";
        // 建立查询解析器
        //searchField:要查询的字段；
        //analyzer:标准分词器实例
        QueryParser parser = new QueryParser(searchField, analyzer);
        Query query = parser.parse(q);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits = indexSearcher.search(query, 10);
        System.out.println("匹配 " + q + "查询到" + hits.totalHits + "个记录");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }

        QueryParser parser2 = new QueryParser(searchField, analyzer);
        Query query2 = parser2.parse(q2);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits2 = indexSearcher.search(query2, 10);
        System.out.println("匹配 " + q2 + "查询到" + hits2.totalHits + "个记录");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }
    }

    @Test
    public void testMultiFieldQuery() throws IOException, ParseException, InvalidTokenOffsetsException {
        String[] fields = {"chapter", "content"}; // 要搜索的字段，一般搜索时都不会只搜索一个字段
        // 字段之间的与或非关系，MUST表示and，MUST_NOT表示not，SHOULD表示or，有几个fields就必须有几个clauses
        BooleanClause.Occur[] clauses = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
        // MultiFieldQueryParser表示多个域解析， 同时可以解析含空格的字符串，如果我们搜索"上海 中国"
        // 创建搜索的Query
        //对违法行为知情不报，不制止，情节轻微。
        Query multiFieldQuery= MultiFieldQueryParser.parse("第八章第五十五条第二款，情节较重。",fields,clauses,analyzer);
        // 根据searcher搜索并且返回TopDocs
        TopDocs topDocs=indexSearcher.search(multiFieldQuery,100);
        System.out.println("共找到匹配处：" + topDocs.totalHits);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("id"));
        }

        //高亮显示
        QueryScorer scorer=new QueryScorer(multiFieldQuery);
        //显示得分高的片段
        Fragmenter fragmenter=new SimpleSpanFragmenter(scorer);
        //设置标签内部关键字的颜色
        //第一个参数：标签的前半部分；第二个参数：标签的后半部分。
        SimpleHTMLFormatter simpleHTMLFormatter=new SimpleHTMLFormatter("<b><font color='red'>","</font></b>");
        //第一个参数是对查到的结果进行实例化；第二个是片段得分（显示得分高的片段，即摘要）
        Highlighter highlighter=new Highlighter(simpleHTMLFormatter,scorer);
        //设置片段
        highlighter.setTextFragmenter(fragmenter);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            String address = doc.get("content");
            //第一个参数是对哪个参数进行设置；第二个是以流的方式读入
            TokenStream tokenStream=analyzer.tokenStream("content",new StringReader(address));
//            CharTermAttribute term=tokenStream.getAttribute(CharTermAttribute.class);
//            //遍历分词数据
//            while(tokenStream.incrementToken()){
//                System.out.print(term.toString()+"|");
//            }
//            System.out.print(term);

            //获取最高的片段
            System.out.println("高亮片段是："+highlighter.getBestFragment(tokenStream, address));
        }
    }

    /**
     * 高亮显示
     *
     * @throws Exception
     */
    @Test
    public void testHighLighter() throws Exception {
        // 标准分词器
        Analyzer analyzer = new IKAnalyzer();
        String searchField = "content";
        String q = "不制止情节较重";
        String q2 = "上海广州人民欢迎你";
        // 建立查询解析器
        //searchField:要查询的字段；
        //analyzer:标准分词器实例
        QueryParser parser = new QueryParser(searchField, analyzer);
        Query query = parser.parse(q);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits = indexSearcher.search(query, 10);
        System.out.println("匹配 " + q + " 查询到 " + hits.totalHits + " 个记录");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("id"));
        }
        //分页查询
//        indexSearcher.searchAfter()

        //高亮显示start
        //算分
        QueryScorer scorer=new QueryScorer(query);
        //显示得分高的片段
        Fragmenter fragmenter=new SimpleSpanFragmenter(scorer);

        //设置标签内部关键字的颜色
        //第一个参数：标签的前半部分；第二个参数：标签的后半部分。
        SimpleHTMLFormatter simpleHTMLFormatter=new SimpleHTMLFormatter("<b><font color='red'>","</font></b>");
        //第一个参数是对查到的结果进行实例化；第二个是片段得分（显示得分高的片段，即摘要）
        Highlighter highlighter=new Highlighter(simpleHTMLFormatter,scorer);
        //设置片段
        highlighter.setTextFragmenter(fragmenter);
        //高亮显示end
        //遍历topDocs
        /**
         * ScoreDoc:是代表一个结果的相关度得分与文档编号等信息的对象。
         * scoreDocs:代表文件的数组
         * @throws Exception
         * */
        for(ScoreDoc scoreDoc:hits.scoreDocs){
            Document document= indexSearcher.doc(scoreDoc.doc);
            System.out.print(document.get("id"));
            System.out.print(document.get("chapter"));
            System.out.println(document.get("content"));

            String address = document.get("content");
            //第一个参数是对哪个参数进行设置；第二个是以流的方式读入
            TokenStream tokenStream=analyzer.tokenStream("content",new StringReader(address));
            //获取最高的片段
            System.out.println("高亮片段是："+highlighter.getBestFragment(tokenStream, address));

            tokenStream.close();
        }
    }
    public static void main(String[] args) {
        //构建IK分词器，使用smart分词模式
        Analyzer analyzer = new IKAnalyzer(true);

        //获取Lucene的TokenStream对象
        TokenStream ts = null;
        try {
            ts = analyzer.tokenStream("myfield", new StringReader("这是一个中文分词的例子，你可以直接运行它！IKAnalyer can analysis english text too"));
            //获取词元位置属性
            OffsetAttribute offset = ts.addAttribute(OffsetAttribute.class);
            //获取词元文本属性
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            //获取词元文本属性
            TypeAttribute type = ts.addAttribute(TypeAttribute.class);

            //重置TokenStream（重置StringReader）
            ts.reset();
            //迭代获取分词结果
            while (ts.incrementToken()) {
                System.out.println(offset.startOffset() + " - " + offset.endOffset() + " : " + term.toString() + " | " + type.type());
            }
            //关闭TokenStream（关闭StringReader）
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //释放TokenStream的所有资源
            if (ts != null) {
                try {
                    ts.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 分页报错，不可用
     * 创建用户:狂飙的yellowcong<br/>
     * 创建日期:2017年12月3日<br/>
     * 创建时间:下午3:54:55<br/>
     * 机能概要:通过IndexSearcher.searchAfter
     */
    @Test
    public void testQueryByPager2() {
        int pageNow=1;
        int pageSize=2;
        try {
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse("chapter:第五十五条");
            // 创建IndexSearcher
            IndexSearcher searcher = new IndexSearcher(reader);

            int start = (pageNow - 1) * pageSize;
            // 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
            TopDocs topDocs = searcher.search(query, start);
            //获取到上一页最后一条
//            ScoreDoc preScore = topDocs.scoreDocs[start-1];
            ScoreDoc preScore = topDocs.scoreDocs[0];

            //查询最后一条后的数据的一页数据
            topDocs = searcher.searchAfter(preScore, query, pageSize);
            ScoreDoc[] scores = topDocs.scoreDocs;

            System.out.println("查询到的条数\t" + topDocs.totalHits);
            //读取数据
            for (int i = 0; i < scores.length; i++) {
                Document doc = reader.document(scores[i].doc);
                System.out.println(doc.get("id") + ":" + doc.get("chapter") + ":" + doc.get("content"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            coloseReader(reader);
        }
    }

    /**
     * 可用
     * 创建用户:狂飙的yellowcong<br/>
     * 创建日期:2017年12月3日<br/>
     * 创建时间:下午3:37:17<br/>
     * 机能概要: 查询所有数据后再进行分页
     */
    @Test
    public void testQueryByPager1(){
        int pageNow=3;
        int pageSize=2;
        try {

            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse("chapter:第五十五条第二款");

            //创建IndexSearcher
            IndexSearcher searcher = new IndexSearcher(reader);

            //查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
            TopDocs topDocs = searcher.search(query, 100);

            System.out.println("查询到的条数\t"+topDocs.totalHits);
            ScoreDoc [] scores = topDocs.scoreDocs;
            for(ScoreDoc scoreDoc:scores){
                System.out.println(indexSearcher.doc(scoreDoc.doc).get("id"));
            }
            System.out.println("分页数据---");
            //查询起始记录位置
            int begin = pageSize * (pageNow - 1) ;
            //查询终止记录位置
            int end = Math.min(begin + pageSize, scores.length);

            //进行分页查询
            for(int i=begin;i<end;i++) {
                int docID = scores[i].doc;
//                Explanation explanation = indexSearcher.explain(query, docID);
//                System.out.println(explanation.toString());
                Document doc = indexSearcher.doc(docID);
                System.out.println(doc.get("id")+":"+doc.get("chapter")+":"+doc.get("content"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
//            coloseReader(reader);
        }
    }
}
