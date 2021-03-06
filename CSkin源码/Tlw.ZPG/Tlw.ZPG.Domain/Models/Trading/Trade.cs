namespace Tlw.ZPG.Domain.Models.Trading
{
    using System;
    using System.Collections.Generic;
    using System.ComponentModel.DataAnnotations;
    using Tlw.ZPG.Domain.Enums;
    using Tlw.ZPG.Domain.Models.Admin;
    using Tlw.ZPG.Domain.Models.Bid;
    using Tlw.ZPG.Infrastructure;
    using Tlw.ZPG.Infrastructure.Utils;

    public partial class Trade : EntityBase
    {
        private const string ENCRYPT_KEY = "87654321";

        public Trade()
        {
            this.TradeMessages = new HashSet<TradeMessage>();
            this.TradeDetails = new HashSet<TradeDetail>();
        }

        #region 属性
        /// <summary>
        /// 宗地id
        /// </summary>
        public int LandId { get; internal set; }
        /// <summary>
        /// 公告id
        /// </summary>
        public int AfficheId { get; internal set; }
        /// <summary>
        /// 创建时间
        /// </summary>
        public System.DateTime CreateTime { get; set; }
        /// <summary>
        /// 修改时间
        /// </summary>
        public System.DateTime? UpdateTime { get; set; }

        /// <summary>
        /// 报名起始时间
        /// </summary>
        public System.DateTime SignBeginTime { get; set; }
        /// <summary>
        /// 报名截止时间
        /// </summary>
        public System.DateTime SignEndTime { get; set; }
        /// <summary>
        /// 交易起始时间
        /// </summary>
        public System.DateTime TradeBeginTime { get; set; }
        /// <summary>
        /// 交易截止时间
        /// </summary>
        public System.DateTime TradeEndTime { get; set; }
        /// <summary>
        /// 起始价
        /// </summary>
        public decimal StartPrice { get; set; }
        /// <summary>
        /// 保留价
        /// </summary>
        internal string ReservePrice { get; set; }
        /// <summary>
        /// 挂牌阶段加价幅度
        /// </summary>
        public decimal HangPriceIncrease { get; set; }
        /// <summary>
        /// 拍卖阶段加价幅度
        /// </summary>
        public decimal AuctionPriceIncrease { get; set; }
        /// <summary>
        /// 当前报价
        /// </summary>
        public decimal CurrentPrice { get; internal set; }
        /// <summary>
        /// 状态
        /// </summary>
        public TradeStatus Status { get; internal set; }
        /// <summary>
        /// 前一个状态
        /// </summary>
        public TradeStatus? PrevStatus { get; internal set; }
        /// <summary>
        /// 状态变动时间
        /// </summary>
        public DateTime StatusTime { get; internal set; }
        /// <summary>
        /// 成交时间
        /// </summary>
        public System.DateTime? DealTime { get; set; }
        /// <summary>
        /// 成交用户id
        /// </summary>
        public int? DealAccountId { get; set; }
        /// <summary>
        /// 创建用户id
        /// </summary>
        public int CreatorId { get; set; }
        /// <summary>
        /// 成交价
        /// </summary>
        public decimal DealPrice { get; set; }
        /// <summary>
        /// 阶段
        /// </summary>
        public TradeStage Stage { get; internal set; }
        /// <summary>
        /// 成交类型
        /// </summary>
        public TradeDealType TradeDealType { get; internal set; }
        /// <summary>
        /// 行政区id
        /// </summary>
        public int CountyId { get; set; }
        /// <summary>
        /// 交易结果公示
        /// </summary>
        public TradeResultAffiche ResultAffiche { get; set; }
        [Timestamp]
        internal byte[] RowVersion { get; set; }

        public virtual Affiche Affiche { get; internal set; }
        public virtual Land Land { get; internal set; }
        public virtual User Creator { get; set; }
        public virtual County County { get; set; }
        public virtual Account DealAccount { get; set; }
        public virtual ICollection<TradeMessage> TradeMessages { get; internal set; }
        public virtual ICollection<TradeLog> TradeLogs { get; internal set; }
        public virtual ICollection<TradeDetail> TradeDetails { get; internal set; }
        public virtual TradeDealConfirm TradeResultConfirm { get; internal set; }
        #endregion

        /// <summary>
        /// 获取顺延时间
        /// </summary>
        /// <returns></returns>
        public DateTime GetPostponeTime()
        {
            return this.TradeEndTime + (DateTime.Now - this.StatusTime);
        }

        /// <summary>
        /// 恢复，解冻
        /// </summary>
        public void Recover(int userId, DateTime tradeEndTime)
        {
            if (this.Status != TradeStatus.Normal)
            {
                if (this.Status == TradeStatus.Froze)
                {
                    if (userId != this.CreatorId) throw new TradeRecoverException("你不是该宗地创建者，无法解冻交易");
                    var canRecoverHours = AppSettings.GetValue("HowManyHoursCanRecoverAfterFrozed", 1);
                    var resumedHours = AppSettings.GetValue("ShouldBeResumedAfterHoursLater", 24);
                    if ((DateTime.Now - this.StatusTime).TotalHours > canRecoverHours)
                        throw new TradeRecoverException(string.Format("冻结超过{0}个小时，必须在{1}小时后解冻", canRecoverHours, resumedHours));
                    if(GetPostponeTime() < tradeEndTime)
                    {
                        throw new TradeRecoverException("顺延时间不能小于冻结时间");
                    }
                    SetStatus(TradeStatus.Normal);
                    this.TradeEndTime = tradeEndTime;
                    //如果是拍卖阶段，解冻后需要回到等待阶段
                    if (this.Stage == TradeStage.Auction)
                    {
                        this.Stage = TradeStage.Ready;
                    }
                }
                else
                {
                    Throw(TradeStatus.Normal);
                }
            }
        }

        /// <summary>
        /// 冻结
        /// </summary>
        public void Froze(int userId, string innerExplain, string outExplain)
        {
            if (this.Status != TradeStatus.Froze)
            {
                if (this.Status == TradeStatus.Normal)
                {
                    if (userId != this.CreatorId) throw new TradeFrozeException("你不是该宗地创建者，无法冻结交易");
                    var minutes = AppSettings.GetValue("CanFrozeOrTerminateBeforeTradeEndTime", 60);
                    if ((this.TradeEndTime - DateTime.Now).TotalMinutes <= minutes)
                    {
                        throw new TradeFrozeException(string.Format("交易最后{0}分钟不能冻结", minutes));
                    }
                    SetStatus(TradeStatus.Froze);
                }
                else
                {
                    Throw(TradeStatus.Froze);
                }
            }
        }

        /// <summary>
        /// 终止
        /// </summary>
        public void Terminate(int userId, string innerExplain, string outExplain)
        {
            if (this.Status != TradeStatus.Terminate)
            {
                if (this.Status == TradeStatus.Normal || this.Status == TradeStatus.Froze)
                {
                    if (userId != this.CreatorId) throw new TradeTerminateException("你不是该宗地创建者，无法终止交易");
                    var minutes = AppSettings.GetValue("CanFrozeOrTerminateBeforeTradeEndTime", 60);
                    if ((this.TradeEndTime - DateTime.Now).TotalMinutes <= minutes)
                    {
                        throw new TradeTerminateException(string.Format("交易最后{0}分钟不能终止", minutes));
                    }
                    SetStatus(TradeStatus.Terminate);
                }
                else
                {
                    Throw(TradeStatus.Terminate);
                }
            }
        }

        private void SetStatus(TradeStatus status)
        {
            this.PrevStatus = this.Status;
            this.StatusTime = DateTime.Now;
            this.Status = status;
        }

        /// <summary>
        /// 竞买人确认
        /// </summary>
        public void ConfirmByBidder(string randomNumber, string applyNumber, int accountId, string ip, string systemInfo)
        {
            if (this.Stage != TradeStage.Complete) throw new ConfirmTradeResultException("确认失败，当前状态不正确");
            if (HasConfirm) throw new ConfirmTradeResultException("你已经确认过，不能重复确认");
            if (this.DealAccountId != accountId) throw new ConfirmTradeResultException("确认失败，你不是该宗地的成交人");
            if (DateTime.Now > this.TradeResultConfirm.ExpiredTime) throw new ConfirmTradeResultException("确认失败，信息已过期");
            if (this.TradeResultConfirm.RandomNum != randomNumber) throw new ConfirmTradeResultException("确认失败，随机码不正确");
            this.TradeResultConfirm.ConfirmTime = DateTime.Now;
            this.TradeResultConfirm.IP = ip;
            this.TradeResultConfirm.SystemInfo = systemInfo;
        }

        private bool HasConfirm
        {
            get
            {
                return this.TradeResultConfirm != null && this.TradeResultConfirm.ConfirmTime.HasValue;
            }
        }

        /// <summary>
        /// 设置保留价
        /// </summary>
        /// <param name="userId"></param>
        /// <param name="price"></param>
        public void SetReservePrice(int userId, decimal price)
        {
            if (price < this.StartPrice) throw new SetReservePriceException("保留价不得低于起始价");
            if (this.Status != TradeStatus.Normal) throw new SetReservePriceException("当前状态不允许录入保留价");
            var hours = AppSettings.GetValue("HowManyHoursCanSetReservePriceBeforeTradeEndTime", 1);
            if ((this.TradeEndTime - DateTime.Now).TotalHours < 1) throw new SetReservePriceException(string.Format("交易结束前{0}个小时不允许录入保留价", hours));
            if (!string.IsNullOrEmpty(this.ReservePrice)) throw new SetReservePriceException("已有保留价，不能再次录入");
            this.ReservePrice = price.ToString();
            this.ReservePrice = SecurityUtil.EncryptTriDes(this.ReservePrice, ENCRYPT_KEY);
        }

        /// <summary>
        /// 判断是否大于保留价
        /// </summary>
        /// <param name="price"></param>
        /// <returns></returns>
        public bool IsGreaterThanReservePrice(decimal price)
        {
            var reservePrice = Convert.ToDecimal(SecurityUtil.DecryptTriDes(this.ReservePrice, ENCRYPT_KEY));
            return price > reservePrice;
        }

        #region 报价
        public void SubmitPrice(decimal price, string applyNumber, int accountId, string ip, string systemInfo)
        {
            if (this.Status != TradeStatus.Normal) throw new SubmitPriceException("当前状态不允许报价");
            if (!(this.Stage == TradeStage.Auction || this.Stage == TradeStage.Hang)) throw new SubmitPriceException("当前阶段不允许报价");
            if (DateTime.Now < this.TradeBeginTime || DateTime.Now > this.TradeEndTime) throw new SubmitPriceException("只能在交易期间才能报价");
            if (this.CurrentPrice == 0 && price < this.StartPrice) throw new SubmitPriceException("首次报价必须大于起始价");
            var priceIncrease = GetPriceIncrease();
            if (price < this.CurrentPrice + priceIncrease) throw new SubmitPriceException("报价不得低于当前阶段报价的最小值");
            AddDetail(price, applyNumber, accountId, ip, systemInfo);
            this.CurrentPrice = price;
        }

        private void AddDetail(decimal price, string applyNumber, int accountId, string ip, string systemInfo)
        {
            this.TradeDetails.Add(new TradeDetail()
            {
                AccountId = accountId,
                ApplyNumber = applyNumber,
                CreateTime = DateTime.Now,
                Trade = this,
                PrevPrice = this.CurrentPrice,
                Price = price,
                SystemInfo = systemInfo,
                IP = ip,
                Remark = string.Format("竞买人：{0}报价", applyNumber),
            });
        }

        private decimal GetPriceIncrease()
        {
            if (this.Stage == TradeStage.Hang)
            {
                return this.HangPriceIncrease;
            }
            return this.AuctionPriceIncrease;
        } 
        #endregion

        private void Throw(TradeStatus status)
        {
            string originalDesc = Tlw.ZPG.Infrastructure.Utils.EnumUtil.GetDescription(this.Status);
            string targetDesc = Tlw.ZPG.Infrastructure.Utils.EnumUtil.GetDescription(status);
            throw new DomainException(string.Format("不允许设置状态，原状态：{0}，目标状态：{1}", originalDesc, targetDesc));
        }
    }
}
