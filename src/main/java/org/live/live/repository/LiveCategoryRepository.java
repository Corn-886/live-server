package org.live.live.repository;

import org.live.common.base.BaseRepository;
import org.live.live.entity.LiveCategory;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Mr.wang on 2017/3/29.
 */
public interface LiveCategoryRepository extends BaseRepository<LiveCategory, String> {

    /**
     * 查询全部的直播分类，按serialNo分类
     * @return
     */
    @Query("select c from LiveCategory c order by c.serailNo ASC")
    public List<LiveCategory> findAllCategory() ;

    /**
     *  获取max的最大值
     * @return
     */
    @Query(value=" select max(l.serailNo) from LiveCategory l")
    public Integer findMaxSerialNo() ;


}