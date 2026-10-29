import {
  Alert,
  Button,
  Card,
  Group,
  Loader,
  Select,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core';
import { usePageTitle } from '@orudesk/web-core';
import { IconDownload } from '@tabler/icons-react';
import { useState } from 'react';
import { financialYearOf, modeLabel, useDonationSummary, useDownloadForm10BD } from '../donations';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** "2026-04" → "Apr 2026" */
const monthLabel = (key: string) => `${MONTHS[Number(key.slice(5, 7)) - 1]} ${key.slice(0, 4)}`;

/** Indian grouping for a plain amount from the API: "150000.50" → "₹1,50,000.50". */
const rupees = (amount: string) =>
  '₹' +
  Number(amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** The year in numbers, and the file for Form 10BD (Day 60). */
export function DonationReportsPage() {
  usePageTitle('Donation reports');
  const current = financialYearOf(new Date());
  const start = Number(current.slice(0, 4));
  const years = [0, 1, 2].map(
    (back) => `${start - back}-${String((start - back + 1) % 100).padStart(2, '0')}`,
  );
  const [year, setYear] = useState(current);
  const summary = useDonationSummary(year);
  const download = useDownloadForm10BD();
  const s = summary.data;

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={1}>Donation reports</Title>
        <Select
          aria-label="Financial year"
          data={years.map((y) => ({ value: y, label: `FY ${y}` }))}
          value={year}
          onChange={(value) => value && setYear(value)}
          allowDeselect={false}
          w={140}
        />
      </Group>
      {summary.isPending && <Loader size="sm" aria-label="Loading" />}
      {summary.isError && <Text role="alert">{summary.error.message}</Text>}
      {s && (
        <>
          <SimpleGrid cols={{ base: 1, sm: 3 }}>
            <Stat label="Received" value={s.totalFormatted} />
            <Stat label="Donations" value={String(s.count)} />
            <Stat label="Cancelled" value={String(s.cancelledCount)} />
          </SimpleGrid>
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
            <Card withBorder>
              <Table aria-label="By month">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Month</Table.Th>
                    <Table.Th ta="right">Donations</Table.Th>
                    <Table.Th ta="right">Amount</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {s.byMonth.map((line) => (
                    <Table.Tr key={line.key}>
                      <Table.Td>{monthLabel(line.key)}</Table.Td>
                      <Table.Td ta="right">{line.count}</Table.Td>
                      <Table.Td ta="right">{rupees(line.total)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Card>
            <Card withBorder>
              <Table aria-label="By payment mode">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Paid by</Table.Th>
                    <Table.Th ta="right">Donations</Table.Th>
                    <Table.Th ta="right">Amount</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {s.byMode.length === 0 && (
                    <Table.Tr>
                      <Table.Td colSpan={3}>No donations this year.</Table.Td>
                    </Table.Tr>
                  )}
                  {s.byMode.map((line) => (
                    <Table.Tr key={line.key}>
                      <Table.Td>{modeLabel(line.key)}</Table.Td>
                      <Table.Td ta="right">{line.count}</Table.Td>
                      <Table.Td ta="right">{rupees(line.total)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Card>
          </SimpleGrid>
        </>
      )}
      <Card withBorder maw={640}>
        <Stack gap="xs">
          <Text fw={500}>Form 10BD</Text>
          <Text size="sm" c="dimmed">
            The statement of 80G-deductible donations, due by 31 May after the year ends. Check the
            file, then upload it on the income tax portal.
          </Text>
          {download.isError && (
            <Alert color="red" role="alert">
              {download.error.message}
            </Alert>
          )}
          <Group>
            <Button
              leftSection={<IconDownload size={16} />}
              variant="default"
              loading={download.isPending}
              onClick={() => download.mutate(year)}
            >
              Download CSV for FY {year}
            </Button>
          </Group>
        </Stack>
      </Card>
    </Stack>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Card withBorder>
      <Text size="sm" c="dimmed">
        {label}
      </Text>
      <Text size="xl" fw={700}>
        {value}
      </Text>
    </Card>
  );
}
