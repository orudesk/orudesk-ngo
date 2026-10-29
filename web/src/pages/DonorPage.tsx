import { Anchor, Badge, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { formatDate, usePageTitle } from '@orudesk/web-core';
import { Link, useParams } from 'react-router';
import { typeLabel, useDonor } from '../donors';

export function DonorPage() {
  const { donorId = '' } = useParams();
  const donor = useDonor(donorId);
  const name = donor.data ? `${donor.data.firstName} ${donor.data.lastName}` : 'Donor';
  usePageTitle(donor.data === null ? 'Donor not found' : name);

  return (
    <Stack>
      <Anchor component={Link} to="/donors" size="sm">
        ← Donors
      </Anchor>
      {donor.isPending && <Loader size="sm" aria-label="Loading" />}
      {donor.isError && <Text role="alert">{donor.error.message}</Text>}
      {donor.data === null && <Title order={1}>Donor not found</Title>}
      {donor.data && (
        <>
          <Group justify="space-between" maw={640}>
            <Group>
              <Title order={1}>{name}</Title>
              <Badge variant="light" tt="none">
                {typeLabel(donor.data.type)}
              </Badge>
            </Group>
            <Group gap="md">
              <Anchor component={Link} to={`/donors/${donor.data.id}/edit`} size="sm">
                Edit
              </Anchor>
              <Anchor component={Link} to={`/activity?subject=${donor.data.id}`} size="sm">
                History
              </Anchor>
            </Group>
          </Group>
          <Card withBorder maw={640}>
            <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md" component="dl" m={0}>
              <Field label="Phone" value={donor.data.phone ?? '—'} />
              <Field label="Email" value={donor.data.email ?? '—'} />
              {/* The API only ever sends the PAN masked (Day 23): the full number never reaches a screen */}
              <Field label="PAN" value={donor.data.panMasked ?? '—'} />
              <Field label="80G receipts" value={donor.data.wantsTaxReceipt ? 'Yes' : 'No'} />
              <Field label="Updates by email" value={donor.data.receivesUpdates ? 'Yes' : 'No'} />
              <Field label="Donor since" value={formatDate(donor.data.registeredOn)} />
            </SimpleGrid>
          </Card>
          <Anchor component={Link} to={`/people/${donor.data.personId}`} size="sm">
            Their entry in the People registry
          </Anchor>
        </>
      )}
    </Stack>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <Text component="dt" size="sm" c="dimmed">
        {label}
      </Text>
      <Text component="dd" m={0}>
        {value}
      </Text>
    </div>
  );
}
