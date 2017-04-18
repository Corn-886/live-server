package org.live.live.service.impl;

import org.live.app.vo.ApplyAnchorVo;
import org.live.common.base.BaseRepository;
import org.live.common.base.BaseServiceImpl;
import org.live.common.response.DataTableModel;
import org.live.common.utils.DataTableUtils;
import org.live.live.entity.Anchor;
import org.live.live.entity.ApplyAnchor;
import org.live.live.entity.LiveRoom;
import org.live.live.entity.MobileUser;
import org.live.live.repository.AnchorRepository;
import org.live.live.repository.ApplyAnchorRepository;
import org.live.live.repository.LiveRoomRepository;
import org.live.live.repository.MobileUserRepository;
import org.live.live.service.ApplyAnchorService;
import org.live.live.service.MobileUserService;
import org.live.live.vo.ApplyInfoVo;
import org.live.live.vo.ApplyVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wang on 2017/4/15.
 */
@Service("applyAnchorService")
public class ApplyAnchorServiceImpl extends BaseServiceImpl<ApplyAnchor, String> implements ApplyAnchorService {

    /**
     * rtmp流地址前缀
     */
    @Value("${system.rtmpAddrPrefix}")
    private String rtmpAddrPrefix ;

    @Resource
    private ApplyAnchorRepository repository;

    @Resource
    private MobileUserRepository mobileUserRepository ;

    @Resource
    private LiveRoomRepository liveRoomRepository ;

    @Resource
    private AnchorRepository anchorRepository ;

    @Override
    protected BaseRepository<ApplyAnchor, String> getRepository() {
        return repository;
    }

    @Override
    public boolean judgeUserApplyCount(String userId, int systemMaxCount) {
        long userApplyCount = repository.countApplyAnchorByUser_Id(userId);
        if (userApplyCount >= systemMaxCount) {
            return false;
        }
        return true;
    }

    @Override
    public void createApplyAnchorForm(ApplyAnchorVo applyAnchorVo) {

    }

    @Override
    public Date getLastApplyAnchorDate(String userId) {
        return repository.getLastApplyAnchorDate(userId) ;
    }

    /**
     * 根据分页信息查询数据
     * @param request
     * @return DataTableModel dataTable定制model
     */
    @Override
    public DataTableModel findByPage(HttpServletRequest request, boolean auditFlag) {
        Map<String, Object> params = DataTableUtils.parseMap(request);
        Page<ApplyVo> page = repository.findApplys((PageRequest) params.get("pageRequest"), (String) params.get("searchVal"), auditFlag) ; // 分页数据
        Long recordsTotal = repository.count(); // 总记录数
        DataTableModel model = DataTableUtils.parseDataTableModel(page, (Integer) params.get("draw"), recordsTotal); // dataTablel定制model
        return model;
    }

    @Override
    public ApplyInfoVo getApplyInfo(String applyId) {
        return repository.getApplyInfo(applyId) ;
    }

    @Transactional
    @Override
    public void saveApplyPassFlag(String applyId, int passFlag, String reason) {
        //TODO 写下推送到手机端，
        ApplyAnchor applyAnchor = repository.getOne(applyId) ;  //当前申请表
        if(passFlag == 2) { //审核未通过
            applyAnchor.setPassFlag(ApplyAnchor.ADUIT_NOT_PASS) ;
            applyAnchor.setNoPassReason(reason) ;
            repository.save(applyAnchor) ;
            return ;
        } else if(passFlag == 1) {  //审核通过
            applyAnchor.setPassFlag(ApplyAnchor.ADUIT_PASS) ;
            MobileUser mobileUser = applyAnchor.getUser();
            mobileUser.setAnchorFlag(true)  ;   //设置移动端用户的主播标志

            Anchor anchorInDB = anchorRepository.findAnchorByUser(mobileUser) ;
            if(anchorInDB != null) {    //此时该申请表的用户已经是主播
                List<ApplyAnchor> applyAnchors = repository.findApplyAnchorsByUser(mobileUser) ;
                for (ApplyAnchor apply: applyAnchors) {
                    if(apply.getPassFlag() == 0) {
                        apply.setPassFlag(ApplyAnchor.ADUIT_IGNORE) ;
                        repository.save(apply) ;
                    }
                }
                return ;
            }

            Anchor anchor = new Anchor() ;  //主播
            anchor.setUser(mobileUser) ;
            anchor.setCreateTime(new Date()) ;
            anchor.setRealName(applyAnchor.getRealName()) ;
            anchor.setIdCard(applyAnchor.getIdCard()) ;

            LiveRoom liveRoom = new LiveRoom() ;    //直播间
            liveRoom.setAnchor(anchor) ;    //主播
            liveRoom.setLiveCategory(applyAnchor.getCategory()) ;   //直播分类
            liveRoom.setRoomNum(mobileUser.getAccount()) ;  //直播间号等于账号，没毛病
            //TODO 这里直播间封面跟直播间名怎么处理
            liveRoom.setLiveRoomUrl(rtmpAddrPrefix + mobileUser.getAccount()) ;

            repository.save(applyAnchor) ;
            mobileUserRepository.save(mobileUser) ;
            anchorRepository.save(anchor) ;
            liveRoomRepository.save(liveRoom) ;
        }

    }


}
