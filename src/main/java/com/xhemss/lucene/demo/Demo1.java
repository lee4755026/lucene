package com.xhemss.lucene.demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by LEE on 2018/10/17 0017 17:02.
 */
public class Demo1 {
    public static void main(String[] args) throws IOException, ParseException {
        /**
         * 创建索引文件
         */
        //创建一个Directory磁盘目录对象，用来存放索引文件
        FSDirectory directory = FSDirectory.open(Paths.get("E:\\xhemss\\lucene\\dataindex"));
        //创建简单中文分析器 创建索引使用的分词器必须和查询时候使用的分词器一样，否则查询不到想要的结果
//        Analyzer analyzer = new SmartChineseAnalyzer(true);
        CharArraySet set=new CharArraySet(1,true);
        set.add("互监组");
        Analyzer analyzer = new SmartChineseAnalyzer(set);
        //实例化索引配置，用来存放分析器
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        //实例化索引写入对象
        IndexWriter indexWriter;

        indexWriter = new IndexWriter(directory, indexWriterConfig);
        //清除以前的index
        indexWriter.deleteAll();
        //创建Document 文档对象
        Document document=new Document();
           /*
            * 参数说明 public Field(String name, String value, FieldType type)
	        * name : 字段名称
	        * value : 字段的值 store :
	        *  TextField.TYPE_STORED:存储字段值
	        */
        document.add(new Field("name", "lin zhengle", TextField.TYPE_STORED));
        document.add(new Field("address", "中国上海", TextField.TYPE_STORED));
        document.add(new Field("dosometing", "I am learning lucene ", TextField.TYPE_STORED));
        indexWriter.addDocument(document);

        document.add(new Field("name", "张三", TextField.TYPE_STORED));
        document.add(new Field("address", "互监组当月无违规，加10分。", TextField.TYPE_STORED));
        document.add(new Field("dosometing", "考核加分", TextField.TYPE_STORED));
        indexWriter.addDocument(document);
        //关闭索引写入对象
        indexWriter.close();


        //搜索
        // 创建IndexSearcher 检索索引的对象，里面要传递上面写入的内存目录对象directory
        DirectoryReader directoryReader=DirectoryReader.open(directory);
        IndexSearcher indexSearcher=new IndexSearcher(directoryReader);
        String[] fields = {"name", "address","dosometing"}; // 要搜索的字段，一般搜索时都不会只搜索一个字段
        // 字段之间的与或非关系，MUST表示and，MUST_NOT表示not，SHOULD表示or，有几个fields就必须有几个clauses
        BooleanClause.Occur[] clauses = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
        // MultiFieldQueryParser表示多个域解析， 同时可以解析含空格的字符串，如果我们搜索"上海 中国"
        // 创建搜索的Query
        Query multiFieldQuery= MultiFieldQueryParser.parse("中国",fields,clauses,analyzer);
        // 根据searcher搜索并且返回TopDocs
        TopDocs topDocs=indexSearcher.search(multiFieldQuery,100);
        System.out.println("共找到匹配处：" + topDocs.totalHits); // totalHits和scoreDocs.length的区别还没搞明白

    }
}
