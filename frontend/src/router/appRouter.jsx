import { BrowserRouter as Router, Routes, Route, useParams } from 'react-router-dom';
import PrivateRoute from '@/components/common/PrivateRoute';
import '@/assets/css/common/appRouter.css';
import MainHeader from '@/components/common/MainHeader';
import Header from '@/components/common/GroupHeader';
import Footer from '@/components/common/GroupFooter';
import Main from '@/pages/Main';
import Login from '@/pages/user/Login';
import Signup from '@/pages/user/Signup';
import AccountSetup from '@/components/signup/AccountSetup';
import RegisterAccount from '@/pages/user/RegisterAccount';
import CreateGroup from '@/pages/createGroup/CreateGroup';
import Profile from '@/pages/user/Profile';
import CreatePost from '@/pages/createPost/CreatePost';
import GroupAccount from '@/pages/groupAccount/GroupAccount';
import Schedule from '@/pages/schedule/GroupSchedule';
import Feed from '@/pages/feed/Feed';
import Post from '@/pages/post/Post';
import GroupAccountDepositStatus from '@/pages/groupAccount/GroupAccountDepositStatus';
import GroupDuesSetting from '@/pages/groupAccount/GroupDuesSetting';
import GroupInfo from '@/pages/groupInfo/GroupInfo';
import AccountTransfer from '@/pages/groupAccount/AccountTransfer';
import AccountTransferQuestion from '@/pages/groupAccount/AccountTransferQuestion';
import AccountTransferCheck from '@/pages/groupAccount/AccountTransferCheck';
import AccountWithdrawal from '@/pages/groupAccount/AccountWithdrawal';
import AccountWithdrawalQuesion from '@/pages/groupAccount/AccountWithdrawalQuestion';
import AccountWithdrawalCheck from '@/pages/groupAccount/AccountWithdrawalCheck';
import GroupAccountSearch from '@/pages/groupAccount/GroupAccountSearch';
import TransactionDetail from '@/pages/groupAccount/TransactionDetail';
import Notice from '@/pages/notification/Notice';
import UserHeader from '@/components/common/UserHeader';
import SearchPost from '@/pages/searchPost/SearchPost';
import React, { useEffect } from 'react';
import data from '../pages/notification/data.json'; // data.json 경로
import useNoticeState from '../store/useNoticeState'; // zustand 스토어

import GetFcmToken from '@/pages/GetFcmToken';
import InviteMember from '../pages/inviteMember/InviteMember';

const AppRouter = () => {
	const { setIsUnreadNotice } = useNoticeState();

	useEffect(() => {
		// 읽지 않은 알림이 있는지 확인하여 상태 업데이트
		const hasUnreadNotice = data.notice.some((notice) => !notice.isRead);
		setIsUnreadNotice(hasUnreadNotice); // 읽지 않은 알림이 있으면 true로 설정
	}, [setIsUnreadNotice]);

	return (
		<Router>
			<MainHeader />
			<Header />
			<UserHeader />
			<main>
				<Routes>
					<Route
						path="/"
						element={
							<PrivateRoute>
								<Main />
							</PrivateRoute>
						}
					/>
					<Route
						path="/login"
						element={<Login />}
					/>
					<Route
						path="/signup"
						element={<Signup />}
					/>
					<Route
						path="/account-setup"
						element={<AccountSetup />}
					/>
					<Route
						path="/registeraccount"
						element={<RegisterAccount />}
					/>
					<Route
						path="/create"
						element={<CreateGroup />}
					/>
					<Route
						path="/profile"
						element={<Profile />}
					/>
					<Route
						path="/notice"
						element={<Notice />}
					/>
					<Route
						path="/group/:groupId/create"
						element={<CreatePost />}
					/>
					<Route
						path="/group/:groupId/account"
						element={<GroupAccount />}
					/>
					<Route
						path="/group/:groupId/schedule"
						element={<Schedule />}
					/>
					<Route
						path="/group/:groupId/info"
						element={<GroupInfo />}
					/>
					<Route
						path="/group/:groupId/invite"
						element={<InviteMember />}
					/>
					<Route
						path="/group/:groupId"
						element={<Feed />}
					/>
					<Route
						path="/group/:groupId/search"
						element={<SearchPost />}
					/>
					<Route
						path="/group/:groupId/:postId"
						element={<Post />}
					/>
					<Route
						path="/group/:groupId/account/status"
						element={<GroupAccountDepositStatus />}
					/>
					<Route
						path="/group/:groupId/account/setting"
						element={<GroupDuesSetting />}
					/>
					<Route
						path="/group/:groupId/account/transfer"
						element={<AccountTransfer />}
					/>
					<Route
						path="/group/:groupId/account/transfer-question"
						element={<AccountTransferQuestion />}
					/>
					<Route
						path="/group/:groupId/account/transfer-check"
						element={<AccountTransferCheck />}
					/>
					<Route
						path="/group/:groupId/account/withdrawal"
						element={<AccountWithdrawal />}
					></Route>
					<Route
						path="/group/:groupId/account/withdrawal-question"
						element={<AccountWithdrawalQuesion />}
					></Route>
					<Route
						path="/group/:groupId/account/withdrawal-check"
						element={<AccountWithdrawalCheck />}
					></Route>
					<Route
						path="/group/:groupId/account/search"
						element={<GroupAccountSearch />}
					/>
					<Route
						path="/group/:groupId/account/:detail"
						element={<TransactionDetail />}
					/>

					<Route
						path="/get-fcm-token"
						element={<GetFcmToken />} // FCM 토큰 받기 위한 페이지 추가
					/>
				</Routes>
			</main>
			<Footer />
		</Router>
	);
};

export default AppRouter;
