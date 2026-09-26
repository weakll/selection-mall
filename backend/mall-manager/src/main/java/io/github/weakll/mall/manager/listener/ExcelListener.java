package io.github.weakll.mall.manager.listener;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import io.github.weakll.mall.manager.mapper.CategoryMapper;
import io.github.weakll.mall.model.vo.product.CategoryExcelVo;
import java.util.List;
public class ExcelListener<T> extends AnalysisEventListener<T> {
    /**
     每隔5条存储数据库，实际使⽤中可以100条，然后清理list ，⽅便内
     存回收
     */
    private static final int BATCH_COUNT = 100;
    /**
     * 缓存的数据
     */
    private List cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    //获取mapper对象
    private CategoryMapper categoryMapper;
    public ExcelListener(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }
    // 每解析⼀⾏数据就会调⽤⼀次该⽅法
    @Override
    public void invoke(T o, AnalysisContext analysisContext) {
        CategoryExcelVo data = (CategoryExcelVo)o;
        cachedDataList.add(data);
        // 达到BATCH_COUNT了，需要去存储⼀次数据库，防⽌数据⼏万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext)
    {
        // excel解析完毕以后需要执⾏的代码
        // 这⾥也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
    }
    private void saveData() {
        categoryMapper.batchInsert(cachedDataList);
    }
}
