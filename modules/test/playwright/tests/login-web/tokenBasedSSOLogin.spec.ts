/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests, request} from '@playwright/test';
import {liferayConfig} from '../../liferay.config';
import {loginTest} from '../../fixtures/loginTest';
import {applicationsMenuPageTest} from "../../fixtures/applicationsMenuPageTest";
import {serverAdministrationPageTest} from "../../fixtures/serverAdministrationPageTest";
import {apiHelpersTest} from "../../fixtures/apiHelpersTest";
import {accountsPagesTest} from "../../fixtures/accountsPagesTest";
import {dataApiHelpersTest} from "../../fixtures/dataApiHelpersTest";
import {VirtualInstancesPage} from '../../pages/portal-instances-web/VirtualInstancesPage';
import performLogin, {performLogout} from "../../utils/performLogin";
import {waitForAlert} from '../../utils/waitForAlert';

export const test = mergeTests(
    accountsPagesTest,
    apiHelpersTest,
    applicationsMenuPageTest,
    dataApiHelpersTest,
    loginTest(),
    serverAdministrationPageTest
);

const DEFAULT_VIRTUAL_INSTANCE_NAME = 'www.able.com';

async function goToTokenBasedSSO(page, settings) {
	await page.getByLabel('Open Applications MenuCtrl+').click();
	await page.getByRole('tab', {name: 'Control Panel'}).click();
	await page.getByRole('menuitem', {name: settings}).click();
	await page.getByRole('link', {name: 'SSO'}).click();
	await page.waitForLoadState();
	await page.getByRole('menuitem', {name: 'Token Based SSO'}).click();
	await page.waitForLoadState();
}

async function resetDefaultSettings(page) {
	if (await page.getByRole('button', { name: 'Actions'}).isVisible()) {
		await page.getByRole('button', {name: 'Actions'}).click();
		await page.getByRole('link', {name: 'Reset Default Values'}).click();
		await page.getByRole('button', {name: 'Save'}).click();
		await expect(
			page.getByText('Info:This configuration is')).toBeVisible();
	}
}

async function enableTokenBasedSSO(page) {
	await page.getByLabel('Enabled').check();
	await page.getByLabel('Token Location').click();
	await page.getByRole('option', {name: 'Request Header'}).click();

	await test.step('Update Token Based SSO Configuration', async () => {
		const updateButton = page.getByRole('button', {
			name: 'Update',
		});

		const saveButton = page.getByRole('button', {
			name: 'Save',
		});

		if (await saveButton.isVisible()) {
			await saveButton.click();
		}
		else if (await updateButton.isVisible()) {
			await updateButton.click();
		}

		await waitForAlert(page);
	});

	await page.waitForTimeout(1000);
}

async function verifyTokenBasedSSO(token: string, url: string) {
	const context = await request.newContext({
		extraHTTPHeaders: {
			'SM_USER': token
		}
	});

	const response = await context.get(url);
	expect(response.status()).toBe(200);
	const responseBody = await response.text();
	expect(responseBody).toContain('Sign In');
	await context.dispose();
}

test.describe('Users could login using Token Based SSO.  See LRQA-27622.', () => {
	test('Verify token based login with default user', async ({
		page,
	}) => {
		await goToTokenBasedSSO(page, 'System Settings');
		await resetDefaultSettings(page);
		await enableTokenBasedSSO(page);

		const token = 'test@liferay.com';
		const url = 'http://localhost:8080/web/guest';

		await verifyTokenBasedSSO(token, url);

		await resetDefaultSettings(page);

		await performLogout(page);
	});

	test('Verify token based login with able.com', async ({
		browser,
		page,
	}) => {
		const virtualInstancesPage = new VirtualInstancesPage(page);

		await virtualInstancesPage.addNewVirtualInstance(
			DEFAULT_VIRTUAL_INSTANCE_NAME
		);

		const defaultBaseUrl = liferayConfig.environment.baseUrl;

		liferayConfig.environment.baseUrl = `http://${DEFAULT_VIRTUAL_INSTANCE_NAME}:8080`;

		const newPage = await browser.newPage({
			baseURL: `http://${DEFAULT_VIRTUAL_INSTANCE_NAME}:8080`,
		});

		await performLogin(
			newPage,
			'test',
			'?p_p_id=com_liferay_login_web_portlet_LoginPortlet&' +
			'p_p_state=maximized',
			`@${DEFAULT_VIRTUAL_INSTANCE_NAME}.com`
		);

		await goToTokenBasedSSO(newPage, 'Instance Settings');

		await newPage.getByLabel('Enabled').check();
		await newPage.getByRole('button', {name: 'Save'}).click();
		await newPage.waitForLoadState();

		const token = `test@${DEFAULT_VIRTUAL_INSTANCE_NAME}`;
		const url = `http://${DEFAULT_VIRTUAL_INSTANCE_NAME}:8080/web/guest`;

		await verifyTokenBasedSSO(token, url);

		await performLogout(newPage);

		liferayConfig.environment.baseUrl = defaultBaseUrl;

		await virtualInstancesPage.deleteVirtualInstance(
			DEFAULT_VIRTUAL_INSTANCE_NAME
		);

		await goToTokenBasedSSO(page, 'System Settings');
		await resetDefaultSettings(page);
	});
});
