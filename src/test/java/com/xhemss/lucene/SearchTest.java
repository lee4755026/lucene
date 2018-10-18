package com.xhemss.lucene;

import java.io.StringReader;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class SearchTest {

    private Directory dir;
    private IndexReader reader;
    private IndexSearcher is;

    @Before
    public void setUp() throws Exception {
        dir=FSDirectory.open(Paths.get("E:\\xhemss\\lucene\\dataindex"));
        reader=DirectoryReader.open(dir);
        is=new IndexSearcher(reader);
    }

    @After
    public void tearDown() throws Exception {
        reader.close();
    }

    /**
     * 对特定单词查询及模糊查询
     *
     * @throws Exception
     */
    @Test
    public void testTermQuery() throws Exception {
        String searchField = "address";
        // 所给出的必须是单词，不然差不到
        String q = "中国";
        // 一个Term表示来自文本的一个单词。
        Term t = new Term(searchField, q);
        // 为Term构造查询。
        Query query = new TermQuery(t);
        /**
         * 1.需要根据条件查询
         *
         * 2.最大可编辑数，取值范围0，1，2
         * 允许我的查询条件的值，可以错误几个字符
         *
         */
        Query query2 = new FuzzyQuery(new Term(searchField,"中国了了"),2);
        TopDocs hits = is.search(query, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        System.out.println("匹配 '" + q + "'，总共查询到" + hits.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }
        TopDocs hits2 = is.search(query2, 10);
        // hits.totalHits：查询的总命中次数。即在几个文档中查到给定单词
        System.out.println("匹配 '" + "中国了了"+ "'，总共查询到" + hits2.totalHits + "个文档");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
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
        TopDocs hits = is.search(query, 10);
        System.out.println("匹配 " + q + "查询到" + hits.totalHits + "个记录");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }

        QueryParser parser2 = new QueryParser(searchField, analyzer);
        Query query2 = parser2.parse(q2);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits2 = is.search(query2, 10);
        System.out.println("匹配 " + q2 + "查询到" + hits2.totalHits + "个记录");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
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
        CharArraySet set=new CharArraySet(1,true);
        set.add("互监组");
//        Analyzer analyzer = new SmartChineseAnalyzer(set);
        Analyzer analyzer = new IKAnalyzer();
        String searchField = "address";
        String q = "互监组";
        String q2 = "上海广州人民欢迎你";
        // 建立查询解析器
        //searchField:要查询的字段；
        //analyzer:标准分词器实例
        QueryParser parser = new QueryParser(searchField, analyzer);
        Query query = parser.parse(q);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits = is.search(query, 10);
        System.out.println("匹配 " + q + " 查询到 " + hits.totalHits + " 个记录");
        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }
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
            Document document=is.doc(scoreDoc.doc);
            System.out.print(document.get("name"));
            System.out.print(document.get("address"));
            System.out.println(document.get("dosometing"));

            String address = document.get("address");
            //第一个参数是对哪个参数进行设置；第二个是以流的方式读入
            TokenStream tokenStream=analyzer.tokenStream("address",new StringReader(address));
            CharTermAttribute term=tokenStream.getAttribute(CharTermAttribute.class);
            //遍历分词数据
            while(tokenStream.incrementToken()){
             System.out.print(term.toString()+"|");
            }
            System.out.print(term);

            //获取最高的片段
            System.out.println("高亮片段是："+highlighter.getBestFragment(tokenStream, address));
        }




       /* QueryParser parser2 = new QueryParser(searchField, analyzer);
        Query query2 = parser2.parse(q2);
        //返回查询到的前10项（查到100个相关内容的话也只会返回10个）
        TopDocs hits2 = is.search(query2, 10);
        System.out.println("匹配 " + q2 + "查询到" + hits2.totalHits + "个记录");
        for (ScoreDoc scoreDoc : hits2.scoreDocs) {
            Document doc = is.doc(scoreDoc.doc);
            System.out.println(doc.get("name"));
        }*/
    }


}
