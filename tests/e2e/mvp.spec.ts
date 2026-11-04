import {test,expect} from '@playwright/test';
test('open-access end-to-end flow with actual National Treasury tenders',async({page,request})=>{
 const status=await request.get('/api/health');
 expect(status.ok()).toBeTruthy();
 const health=await status.json();
 expect(health.ok).toBe(true);
 expect(health.count).toBeGreaterThan(0);

 const searchApi=await request.get('/api/tenders?status=open&pageSize=3');
 expect(searchApi.ok()).toBeTruthy();
 const result=await searchApi.json();
 expect(result.total).toBeGreaterThan(0);
 const first=result.items[0];
 expect(first.id).toBeTruthy();
 expect(first.title).toBeTruthy();
 expect(first.source_url).toMatch(/^https:\/\//);

 const refreshed=await request.post('/api/tenders/refresh',{data:{ids:[first.id]}});
 expect(refreshed.ok()).toBeTruthy();
 const refreshedData=await refreshed.json();
 expect(refreshedData.items).toHaveLength(1);
 expect(refreshedData.items[0].id).toBe(first.id);

 await page.goto('/');
 await expect(page.getByRole('heading',{name:/Know what matters/})).toBeVisible();
 await expect(page.getByRole('link',{name:/Find tenders/}).first()).toBeVisible();

 await page.goto('/search?status=open');
 await expect(page.getByRole('heading',{name:'Find tenders'})).toBeVisible();
 await expect(page.locator('article').first()).toBeVisible();
 const query=first.title.split(/\W+/).find((word:string)=>word.length>=6)??first.reference??'tender';
 await page.locator('input[name="q"]').fill(query);
 await page.getByRole('button',{name:'Search',exact:true}).click();
 await expect(page.locator('article').first()).toBeVisible();

 await page.goto('/tenders/'+encodeURIComponent(first.id));
 await expect(page.getByRole('heading',{name:first.title})).toBeVisible();
 await expect(page.getByRole('heading',{name:/Tender documents/})).toBeVisible();
 await page.getByRole('button',{name:'Save',exact:true}).click();
 await expect(page.getByRole('button',{name:'Saved',exact:true})).toBeVisible();
 await page.getByRole('button',{name:/Track bid/}).click();
 await expect(page.getByRole('button',{name:/In pipeline/})).toBeVisible();

 await page.goto('/saved');
 await expect(page.getByRole('heading',{name:'Saved tenders'})).toBeVisible();
 await expect(page.locator('article').first()).toContainText(first.title);
 await page.reload();
 await expect(page.locator('article').first()).toContainText(first.title);
 const refreshButton=page.getByRole('button',{name:'Refresh saved'});
 await expect(refreshButton).toBeEnabled();
 await refreshButton.click();
 await expect(page.getByText('Saved notices checked against live data.')).toBeVisible();

 await page.goto('/pipeline');
 await expect(page.locator('article').first()).toContainText(first.title);
 await page.getByLabel('Bid stage').selectOption('Review');
 await page.getByLabel('Your notes (autosaved)').fill('Tested with official imported tender.');
 await page.reload();
 await expect(page.getByLabel('Bid stage')).toHaveValue('Review');
 await expect(page.getByLabel('Your notes (autosaved)')).toHaveValue('Tested with official imported tender.');

 await page.goto('/research');
 await expect(page.getByRole('heading',{name:'Research'})).toBeVisible();
 await expect(page.getByRole('heading',{name:'Most active buyers'})).toBeVisible();

 await page.goto('/calendar');
 await expect(page.getByRole('heading',{name:'Deadline calendar'})).toBeVisible();

 await page.goto('/settings');
 await expect(page.getByText('Database connected')).toBeVisible();
 await page.getByLabel('In-app alert window').selectOption('3');
 await page.reload();
 await expect(page.getByLabel('In-app alert window')).toHaveValue('3');

 await page.goto('/alerts');
 await expect(page.getByRole('heading',{name:'Deadline alerts'})).toBeVisible();
});
