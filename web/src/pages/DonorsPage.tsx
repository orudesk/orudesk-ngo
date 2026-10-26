import {
  Anchor,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { usePageTitle } from '@orudesk/web-core';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { DONOR_TYPES, typeLabel, useDonorCounts, useDonors, type DonorType } from '../donors';

const ALL = 'ALL';

export function DonorsPage() {
  usePageTitle('Donors');
  const [search, setSearch] = useState('');
  const [debounced] = useDebouncedValue(search.trim(), 300);
  const [type, setType] = useState<DonorType | null>(null);
  const donors = useDonors(debounced, type);
  const counts = useDonorCounts();
  const rows = donors.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={1}>Donors</Title>
        <Button component={Link} to="/donors/new" leftSection={<IconPlus size={16} />}>
          Add donor
        </Button>
      </Group>

      {counts.data && (
        <SimpleGrid cols={{ base: 1, xs: 3 }} maw={720} aria-label="Donors by type">
          {DONOR_TYPES.map(({ value, label }) => {
            const count = counts.data.find((c) => c.type === value);
            return (
              <Card key={value} withBorder padding="md">
                <Text size="sm" c="dimmed">
                  {label}
                </Text>
                <Text fw={700} size="xl">
                  {count?.donors ?? 0}
                </Text>
                <Text size="xs" c="dimmed">
                  {count?.wantingTaxReceipts ?? 0} want 80G receipts
                </Text>
              </Card>
            );
          })}
        </SimpleGrid>
      )}

      <Group align="flex-end">
        <TextInput
          label="Search"
          description="Part of a name, phone number or email"
          leftSection={<IconSearch size={16} />}
          value={search}
          onChange={(event) => setSearch(event.currentTarget.value)}
          w={320}
        />
        <SegmentedControl
          aria-label="Donor type"
          value={type ?? ALL}
          onChange={(value) => setType(value === ALL ? null : (value as DonorType))}
          data={[{ value: ALL, label: 'All' }, ...DONOR_TYPES]}
        />
      </Group>

      {donors.isPending && <Loader size="sm" aria-label="Loading" />}
      {donors.isError && <Text role="alert">{donors.error.message}</Text>}
      {donors.isSuccess && rows.length === 0 && <Text>No donors found.</Text>}

      {rows.length > 0 && (
        <Table.ScrollContainer minWidth={600}>
          <Table striped highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th>Type</Table.Th>
                <Table.Th>Phone</Table.Th>
                <Table.Th>80G receipt</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((donor) => (
                <Table.Tr key={donor.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/donors/${donor.id}`}>
                      {donor.lastName}, {donor.firstName}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>
                    <Badge variant="light" tt="none">
                      {typeLabel(donor.type)}
                    </Badge>
                  </Table.Td>
                  <Table.Td>{donor.phone ?? '—'}</Table.Td>
                  <Table.Td>{donor.wantsTaxReceipt ? 'Yes' : 'No'}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}

      {donors.hasNextPage && (
        <Group>
          <Button
            variant="default"
            onClick={() => void donors.fetchNextPage()}
            loading={donors.isFetchingNextPage}
          >
            Load more
          </Button>
        </Group>
      )}
    </Stack>
  );
}
