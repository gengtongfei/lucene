package com.shinowit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.*;
import java.util.Date;

/**
 * Created by Administrator on 2015/1/20.
 */
public class LuceneTest {

    public static void createIndex() throws Exception {
        File indexDir = new File("D://luceneIndex");
        //dataDir is the directory that hosts the text files that to be indexed
        File dataDir = new File("D://唐家三少");
        Analyzer analyzer = new IKAnalyzer();
        File[] dataFiles = dataDir.listFiles();
        //有文件系统或者内存存储方式，这里使用文件系统存储索引数据
        Directory directory = new NIOFSDirectory(indexDir);

        //生成全文索引的配置对象
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);

        //设置生成全文索引的方式为创建或者追加
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        //创建真正生成全文索引的writer对象
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        long startTime = new Date().getTime();

        for (int i = 0; i < dataFiles.length; i++) {

            if (dataFiles[i].isFile() && dataFiles[i].getName().endsWith(".txt")) {
                System.out.println("Indexing file " + dataFiles[i].getCanonicalPath());
                Document document = new Document();
                Reader txtReader = new FileReader(dataFiles[i]);
                StringField field_filename = new StringField("filename", dataFiles[i].getName(), Field.Store.YES);
                document.add(field_filename);

                TextField txt_content_field = new TextField("content", getFileReaderContent(dataFiles[i].getPath()), Field.Store.YES);

                document.add(txt_content_field);

                indexWriter.addDocument(document);
            }
        }

        indexWriter.commit();
        indexWriter.close();
        long endTime = new Date().getTime();

        System.out.println("It takes " + (endTime - startTime)
                + " milliseconds to create index for the files in directory "
                + dataDir.getPath());

    }

    public static String getFileReaderContent(String filePath){
        StringBuffer sb= new StringBuffer();
        BufferedReader reader= null;
        try {
            // 指定读取文件的编码格式，要和写入的格式一致，以免出现中文乱码,
            reader =new BufferedReader(new InputStreamReader(new FileInputStream(filePath),"gb2312"));
            String str =null;
            while((str= reader.readLine())!= null){
                sb.append(new String(str.getBytes(),"utf-8"));

            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return  sb.toString();
    }

    public static void query(int pagesize,int pageIndex,String queryString) throws Exception{
        File indexDir = new File("D://luceneIndex");
        Analyzer analyzer = new IKAnalyzer();
        //有文件系统或者内存存储方式，这里使用文件系统存储索引数据
        Directory directory = new NIOFSDirectory(indexDir);

        IndexReader indexReader = DirectoryReader.open(directory);
        //创建搜索类
        IndexSearcher indexSearcher =new IndexSearcher(indexReader);

        QueryParser queryParser=new MultiFieldQueryParser(Version.LUCENE_4_10_3,new String[]{"filename","content"},analyzer);
        queryParser.setDefaultOperator(QueryParser.OR_OPERATOR);//多个关键字时采取 or 操作

        Query query=queryParser.parse(queryString);

        int start=(pageIndex -1) * pagesize;
        int max_result_size= start +pagesize;
        TopScoreDocCollector topDocs=TopScoreDocCollector.create(max_result_size,false);
        indexSearcher.search(query,topDocs);

        int rowCount= topDocs.getTotalHits();//满足条件的总记录数
        int pages = (rowCount - 1)/pagesize +1;//计算总页数

        System.out.println("查到的满足查询条件的记录:" + rowCount);

        System.out.println("满足查询条件的分页页数:" + pages);

        TopDocs tds= topDocs.topDocs(start,pagesize);
        ScoreDoc[] scoreDoc =tds.scoreDocs;


        //关键字高亮显示的html标签，需要导入lucene-highlighter-x.x.x.jar
        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<font color='red'>","</font>");
        Highlighter highlighter =new Highlighter(simpleHTMLFormatter,new QueryScorer(query));

        for(int i=0;i<scoreDoc.length;i++){
            //内部编号
            int doc_id=scoreDoc[i].doc;

            //根据文档id找到文档
            Document mydoc= indexSearcher.doc(doc_id);
            //内容增加高亮显示

            TokenStream tokenStream2= analyzer.tokenStream("content",new StringReader(mydoc.get("content")));
            String content = highlighter.getBestFragment(tokenStream2,mydoc.get("content"));

            System.out.println("对应的小说文件名称 :" + mydoc.get("filename") + "高亮内容:" +content);
        }

    }


    public static void main(String args[]) throws Exception {
//        createIndex();
        query(10,1,"美女");

    }
}