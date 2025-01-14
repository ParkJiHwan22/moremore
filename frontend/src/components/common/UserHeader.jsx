import React from 'react';
import usePageName from '../../store/usePageName';
import chevron from '@/assets/img/common/mainHeader/chevron-left.svg';
import { useLocation, useNavigate } from 'react-router-dom';

const UserHeader = () => {
	const { pageName } = usePageName();
	const location = useLocation();
	const navigate = useNavigate();

	if (location.pathname !== '/profile' && location.pathname !== '/notice' && location.pathname !== '/create') return null;

	return (
		<header className="common-header">
			<div className='menu'>
				<img
					className="group-header-back-button-img"
					alt="Back"
					src={chevron}
					onClick={() => navigate(-1)}
				/>
				<div className="groupName">{pageName}</div>
				<div className='group-header-space'></div>
			</div>
		</header>
	);
};

export default UserHeader;
